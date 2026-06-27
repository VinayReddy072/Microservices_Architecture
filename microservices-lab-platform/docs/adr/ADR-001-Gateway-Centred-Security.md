# ADR-001: Gateway-Centred Security Architecture

**Status:** Accepted  
**Date:** 2024-06-17  
**Deciders:** Platform Architect, Security Lead, Backend Lead  
**Supersedes:** None  

---

## Context

The University Lab Equipment Booking Platform exposes two domain services (Booking Service, Equipment Service) to external clients. Security must be enforced consistently, with clear authentication and role-based access control.

**Forces in tension:**
- JWT validation is cryptographically expensive — doing it in every service wastes CPU
- Services accessed directly (bypassing the gateway) should still be protected
- All external traffic enters through the API Gateway (`api-gateway`, port 8080)
- Config Server serves secrets — JWT secret must not be scattered across services

**Key questions:**
1. Where should JWT validation occur — gateway only, every service, or hybrid?
2. How should identity (username, role) propagate between services?
3. How do downstream Feign calls between services carry identity?

---

## Decision

**Enforce JWT validation at the API Gateway as the PRIMARY authentication layer. Propagate validated identity to downstream services via trusted internal HTTP headers.**

```
Client → [API Gateway] → validates JWT → adds X-Auth-Username, X-Auth-Role → [Booking/Equipment Service]
```

Downstream services implement a SECONDARY security layer that:
1. Reads `X-Auth-Username` / `X-Auth-Role` headers (gateway traffic — no JWT re-validation)
2. Falls back to full JWT validation when called directly without those headers (dev/testing)

---

## Alternatives Considered

### Alternative A: Validate JWT at Every Service (Decentralised)
- **Pros:** No gateway SPOF; services fully self-contained
- **Cons:** JWT validation logic duplicated in 3+ services; secret rotation requires updating all; configuration drift risk
- **Rejected:** Duplication and operational complexity outweigh benefits at this scale

### Alternative B: Dedicated OAuth2/OIDC Auth Server (e.g., Keycloak)
- **Pros:** Industry standard; supports token introspection, refresh tokens, PKCE
- **Cons:** Significant additional infrastructure; requires Keycloak cluster; out of scope for this assessment platform
- **Rejected:** Over-engineering for the assessment; recommended as production upgrade path

### Alternative C: Spring Security OAuth2 Resource Server (JWKS)
- **Pros:** Built-in JWT validation; reactive-compatible with WebFlux gateway
- **Cons:** Requires JWKS endpoint or public key distribution; slightly more setup
- **Future consideration:** Preferred upgrade path for production

---

## Consequences

### Positive
- **Single enforcement point** — security policy changes only in `JwtAuthenticationFilter`
- **No secret duplication** — `JWT_SECRET` served by Config Server to gateway only
- **Reduced latency** — downstream services skip expensive HMAC-SHA256 operations
- **Consistent 401/403 format** — always from `GlobalExceptionHandler` via `ApiErrorResponse`

### Negative
- **Gateway becomes critical path** — SPOF (mitigated by horizontal scaling)
- **Internal network trust** — services trust Docker bridge network isolation
- **Header spoofing risk** — mitigated by ensuring services are not directly internet-exposed

### Risk Matrix
| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Gateway bypass (direct service access) | Medium | High | Secondary JWT filter in each service |
| JWT secret compromise | Low | Critical | Env var injection; `JWT_SECRET` rotation via Config Server |
| Gateway SPOF | Low | High | Multiple replicas behind load balancer in prod |

---

## Implementation Evidence

### Java Classes

| Class | Location | Role |
|-------|----------|------|
| `JwtAuthenticationFilter` | `api-gateway/.../filter/JwtAuthenticationFilter.java` | Global filter — validates JWT, propagates headers. Runs at `HIGHEST_PRECEDENCE + 2` |
| `JwtUtil` (gateway) | `api-gateway/.../security/JwtUtil.java` | Validates token using `Jwts.parser().verifyWith(secretKey)` (JJWT 0.12.x) |
| `JwtUtil` (booking) | `booking-service/.../security/JwtUtil.java` | Generates tokens (login) + validates (direct access) |
| `SecurityConfig` (booking) | `booking-service/.../security/SecurityConfig.java` | Stateless; reads `X-Auth-Username`/`X-Auth-Role` from gateway |
| `SecurityConfig` (equipment) | `equipment-service/.../security/SecurityConfig.java` | Same dual-mode trust pattern |
| `AuthController` | `booking-service/.../controller/AuthController.java` | `POST /api/auth/login` → issues JWT |

### Config Files

| File | Key Setting |
|------|------------|
| `config-repo/application.yml` | `jwt.secret: ${JWT_SECRET}` — injected from env, never hardcoded |
| `config-repo/booking-service-dev.yml` | `feign.circuitbreaker.enabled: true` |
| `config-repo/gateway-dev.yml` | Routes: `lb://booking-service`, `lb://equipment-service` |
| `.env.example` | `JWT_SECRET=dev-secret-key-must-be-at-least-256-bits-long...` |

### Filter Chain (execution order)
```
Incoming Request
     ↓
CorrelationIdFilter  (@Order = HIGHEST_PRECEDENCE)     → generates X-Correlation-Id
     ↓
RequestLoggingFilter (@Order = HIGHEST_PRECEDENCE + 1) → logs request + correlation ID
     ↓
JwtAuthenticationFilter (@Order = HIGHEST_PRECEDENCE + 2) → validates JWT, sets X-Auth-Username/Role
     ↓
Route Forwarding → Booking Service / Equipment Service
```

### Role Access Matrix

| Endpoint | ROLE_USER | ROLE_ADMIN | HTTP Status (no token) |
|----------|-----------|------------|----------------------|
| `GET /api/bookings` | ✅ 200 | ✅ 200 | ❌ 401 |
| `GET /api/equipment` | ✅ 200 | ✅ 200 | ❌ 401 |
| `POST /api/bookings` | ✅ 201 | ✅ 201 | ❌ 401 |
| `POST /api/equipment` | ❌ 403 | ✅ 201 | ❌ 401 |
| `PUT /api/bookings/{id}` | ❌ 403 | ✅ 200 | ❌ 401 |
| `PUT /api/equipment/{id}` | ❌ 403 | ✅ 200 | ❌ 401 |
| `DELETE /api/bookings/{id}` | ❌ 403 | ✅ 204 | ❌ 401 |
| `DELETE /api/equipment/{id}` | ❌ 403 | ✅ 204 | ❌ 401 |

### Timestamp References
- `JwtAuthenticationFilter` created: Sprint 1, commit `feat(gateway): add GlobalFilter for JWT validation`
- `SecurityConfig` (both services) created: Sprint 2, commit `feat(booking): add JWT security layer`
- JJWT upgraded from 0.11 → 0.12.x: Sprint 3, commit `fix: update JJWT to 0.12.x API (parserBuilder→parser)`
