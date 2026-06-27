# ADR-003: Resilience and Failure Handling Strategy

**Status:** Accepted  
**Date:** 2024-06-17  
**Deciders:** Platform Architect, Backend Lead, SRE  
**Supersedes:** None  

---

## Context

The Booking Service makes a synchronous Feign call to the Equipment Service to check availability. This creates a **distributed dependency** — if Equipment Service is slow or unavailable:
- Booking Service threads block waiting for HTTP response
- Thread pool exhaustion cascades to all booking requests
- Cascading failure brings down Booking Service too

Without resilience patterns, a single slow downstream service can cause a total platform outage.

**Design requirements:**
- Booking Service must degrade gracefully when Equipment Service is unavailable
- Repeated failures must not consume infinite threads (thread pool protection)
- The system must self-heal when Equipment Service recovers
- All fallback paths must return meaningful responses, not generic 500 errors

---

## Decision

**Apply a three-layer resilience defence using Resilience4J: Retry → Circuit Breaker → Fallback.**

```
BookingService.checkEquipmentAvailability()
        ↓
   [@Retry] max 3 attempts, 500ms wait — retries transient failures
        ↓ (if all retries exhausted OR circuit OPEN)
   [Circuit Breaker] opens when ≥50% of last 10 calls fail
        ↓ (circuit OPEN — no call made)
   [Fallback] EquipmentClientFallback.checkAvailability()
        ↓
   ServiceUnavailableException → GlobalExceptionHandler → 503 response
```

The Feign read-timeout (3000ms) acts as the implicit TimeLimiter — any call taking >3s throws `ReadTimeoutException`, which counts as a failure for the circuit breaker.

---

## Alternatives Considered

### Alternative A: No Resilience (fail-fast on timeout)
- **Pros:** Simple; no added dependency
- **Cons:** Thread exhaustion; cascading failures; poor user experience
- **Rejected:** Unacceptable reliability for production

### Alternative B: Hystrix (Netflix)
- **Pros:** Mature; widely documented
- **Cons:** Netflix put Hystrix in maintenance mode 2018; not compatible with Spring Boot 3.x; no reactive support
- **Rejected:** Deprecated; Resilience4J is the recommended replacement

### Alternative C: Bulkhead Pattern Only
- **Pros:** Isolates thread pools; prevents cascading failures
- **Cons:** Does not handle transient failures or slow responses alone
- **Rejected:** Incomplete solution without circuit breaker and retry

### Alternative D: Retry + Circuit Breaker + Bulkhead + RateLimiter (full suite)
- **Pros:** Maximum protection
- **Cons:** Complex configuration; bulkhead and rate limiter add overhead for this scale
- **Partial adoption:** Implemented Retry + Circuit Breaker; rate limiter added at Gateway level

---

## Consequences

### Positive
- **Self-healing** — circuit breaker automatically transitions OPEN → HALF_OPEN → CLOSED when Equipment Service recovers
- **Fast failure** — OPEN circuit rejects immediately without making HTTP calls (no thread waste)
- **Meaningful fallback** — users get clear 503 instead of raw connection errors
- **Retry handles transient issues** — brief network blips resolved without user impact

### Negative
- **Stale availability** — when circuit is OPEN, bookings are rejected even if Equipment Service just recovered
- **Half-open probe delay** — recovery takes time (wait-duration-in-open-state: 10s dev, 30s prod)
- **Retry amplification** — 3 retries × multiple concurrent users = 3x load on Equipment Service

### Failure Scenario Playbook

| Scenario | Expected Behaviour | Recovery |
|----------|-------------------|---------|
| Equipment Service DOWN | After 10 calls, circuit OPENS → fallback 503 | Auto-close after 10s + 3 probe calls |
| Equipment Service SLOW (>3s) | Feign read-timeout → counts as failure | Same as above |
| Equipment Service returns 500 | Counted as failure; retry up to 3x | Same circuit breaker |
| Equipment Service returns 404 | Not a failure (valid response) | No circuit breaker activation |

---

## Implementation Evidence

### Java Classes

| Class | Location | Role |
|-------|----------|------|
| `EquipmentClient` | `booking-service/.../client/EquipmentClient.java` | Feign interface with `fallback = EquipmentClientFallback.class` |
| `EquipmentClientFallback` | `booking-service/.../client/EquipmentClientFallback.java` | Returns `available=false` with reason when circuit opens |
| `BookingService` | `booking-service/.../service/BookingService.java` | `@Retry(name="equipment-service")` on `checkEquipmentAvailability()` |
| `Resilience4jConfig` | `booking-service/.../config/Resilience4jConfig.java` | Programmatic configuration for circuit breaker registry |
| `GlobalExceptionHandler` | `booking-service/.../exception/GlobalExceptionHandler.java` | Maps `ServiceUnavailableException` → 503, `FeignException` → 503/422 |

### Config Files

| File | Key Setting |
|------|------------|
| `config-repo/booking-service-dev.yml` | `resilience4j.retry.instances.equipment-service.max-attempts: 3` |
| `config-repo/booking-service-dev.yml` | `resilience4j.retry.instances.equipment-service.wait-duration: 500ms` |
| `config-repo/booking-service-dev.yml` | `resilience4j.circuit-breaker.instances.equipment-service.failure-rate-threshold: 50` |
| `config-repo/booking-service-dev.yml` | `resilience4j.circuit-breaker.instances.equipment-service.sliding-window-size: 10` |
| `config-repo/booking-service-dev.yml` | `resilience4j.circuit-breaker.instances.equipment-service.wait-duration-in-open-state: 10s` |
| `config-repo/booking-service-dev.yml` | `feign.client.config.equipment-service.read-timeout: 3000` ← acts as TimeLimiter |
| `config-repo/booking-service-dev.yml` | `feign.circuitbreaker.enabled: true` ← REQUIRED to activate fallback class |
| `config-repo/booking-service-prod.yml` | `wait-duration-in-open-state: 30s` ← longer in prod |

### Resilience Configuration Summary

```yaml
# booking-service-dev.yml
feign:
  circuitbreaker:
    enabled: true          # enables EquipmentClientFallback activation

resilience4j:
  retry:
    instances:
      equipment-service:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - feign.FeignException.ServiceUnavailable
  circuit-breaker:
    instances:
      equipment-service:
        sliding-window-size: 10
        failure-rate-threshold: 50           # opens at 50% failure
        wait-duration-in-open-state: 10s    # probe after 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
```

### Failure Demonstration Commands

**Scenario 1 — Stop Equipment Service (circuit opens):**
```bash
docker-compose stop equipment-service
# Make 10+ booking attempts → observe fallback 503 responses
# Check logs: "Circuit breaker state: OPEN"
docker-compose start equipment-service
# After 10s: circuit moves to HALF_OPEN → probes → CLOSED
```

**Scenario 2 — Simulate slow Equipment Service (timeout):**
```bash
# Add 4s Thread.sleep to EquipmentController.checkAvailability()
# Feign read-timeout=3000ms → ReadTimeoutException counts as failure
# After 10 calls: circuit OPENS
```

**Scenario 3 — Verify fallback response:**
```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Equipment Service is currently unavailable. Please try again later.",
  "traceId": "abc123..."
}
```

### Actuator Monitoring Endpoints

```
GET /actuator/circuitbreakers → circuit breaker state
GET /actuator/retries → retry metrics
GET /actuator/health → overall health including circuit breakers
```

### Timestamp References
- `EquipmentClient` + `EquipmentClientFallback` created: Sprint 2, commit `feat(booking): add EquipmentClient (OpenFeign) with fallback`
- `Resilience4jConfig` created: Sprint 2, commit `feat(booking): configure Resilience4J circuit breaker, retry`
- `feign.circuitbreaker.enabled` added: Sprint 3 audit, commit `fix: add feign.circuitbreaker.enabled to activate fallback`
- `FeignException` handler added: Sprint 3 audit, commit `feat(booking): add FeignException handler to GlobalExceptionHandler`
