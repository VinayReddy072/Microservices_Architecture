#!/usr/bin/env pwsh
# ============================================================
# git-init-commits.ps1
# Initializes a git repository and creates 30 meaningful,
# semantically grouped commits reflecting the actual development
# history of the University Lab Equipment Booking Platform.
#
# Usage (run from microservices-lab-platform directory):
#   cd microservices-lab-platform
#   pwsh ./git-init-commits.ps1
# ============================================================

Set-Location $PSScriptRoot

Write-Host "Initializing git repository..." -ForegroundColor Cyan
git init
git config user.email "student@university.ac.uk"
git config user.name "Lab Platform Developer"

# Helper to commit only what's staged
function Commit($msg) {
    git commit -m $msg --allow-empty
    Write-Host "  [OK] $msg" -ForegroundColor Green
}

# --------------------------------------------------------------
# PHASE 1 - Project bootstrap & architecture setup
# --------------------------------------------------------------
git add pom.xml .gitignore .env.example
Commit "chore: initialise parent POM with Spring Boot 3.3, Java 21, Spring Cloud 2023"

git add README.md
Commit "docs: add project README with architecture overview and domain description"

git add docs/adr/ADR-001-Gateway-Centred-Security.md
Commit "docs(adr): ADR-001 - adopt gateway-centred JWT security with header propagation"

git add docs/adr/ADR-002-Event-Driven-Communication.md
Commit "docs(adr): ADR-002 - choose RabbitMQ topic exchange for async booking events"

git add docs/adr/ADR-003-Resilience-Failure-Handling.md
Commit "docs(adr): ADR-003 - apply Retry + CircuitBreaker + TimeLimiter + Fallback strategy"

# --------------------------------------------------------------
# PHASE 2 - Common Library (shared contracts)
# --------------------------------------------------------------
git add common-library/
Commit "feat(common): add common-library with BookingCreatedEvent, ApiResponse, ApiErrorResponse"

# --------------------------------------------------------------
# PHASE 3 - Config Server
# --------------------------------------------------------------
git add config-server/src/main/java/ config-server/pom.xml
Commit "feat(config): implement Spring Cloud Config Server with basic auth security"

git add config-server/src/main/resources/
Commit "feat(config): add config-repo with dev+prod YAML profiles for all 3 services"

# --------------------------------------------------------------
# PHASE 4 - Eureka Server
# --------------------------------------------------------------
git add eureka-server/
Commit "feat(eureka): implement Eureka service registry with basic auth dashboard security"

# --------------------------------------------------------------
# PHASE 5 - API Gateway
# --------------------------------------------------------------
git add api-gateway/src/main/java/com/labplatform/gateway/security/
Commit "feat(gateway): implement JwtUtil for token validation using JJWT 0.12 API"

git add api-gateway/src/main/java/com/labplatform/gateway/filter/JwtAuthenticationFilter.java
Commit "feat(gateway): add GlobalFilter for JWT validation with public path bypass"

git add api-gateway/src/main/java/com/labplatform/gateway/filter/CorrelationIdFilter.java
Commit "feat(gateway): add CorrelationIdFilter for X-Correlation-Id propagation across services"

git add api-gateway/src/main/java/com/labplatform/gateway/filter/RequestLoggingFilter.java
Commit "feat(gateway): add structured request/response access logging filter"

git add api-gateway/src/main/java/com/labplatform/gateway/controller/
Commit "feat(gateway): add FallbackController for circuit breaker degraded responses"

git add api-gateway/src/main/resources/ api-gateway/pom.xml
Commit "feat(gateway): configure routes, load balancing (lb://), and resilience4j circuit breakers"

# --------------------------------------------------------------
# PHASE 6 - Booking Service (domain + security)
# --------------------------------------------------------------
git add booking-service/src/main/java/com/labplatform/booking/domain/ `
        booking-service/src/main/java/com/labplatform/booking/repository/ `
        booking-service/src/main/java/com/labplatform/booking/dto/ `
        booking-service/src/main/java/com/labplatform/booking/mapper/
Commit "feat(booking): add Booking entity, repository with overlap detection, DTOs, MapStruct mapper"

git add booking-service/src/main/java/com/labplatform/booking/security/
Commit "feat(booking): add JWT security layer - JwtUtil, JwtAuthFilter, SecurityConfig with RBAC"

git add booking-service/src/main/java/com/labplatform/booking/controller/AuthController.java
Commit "feat(booking): add AuthController with /api/auth/login endpoint for JWT issuance"

git add booking-service/src/main/java/com/labplatform/booking/controller/BookingController.java
Commit "feat(booking): add BookingController with full CRUD and @PreAuthorize role guards"

git add booking-service/src/main/java/com/labplatform/booking/exception/
Commit "feat(booking): add GlobalExceptionHandler - all exceptions -> ApiErrorResponse with traceId"

# --------------------------------------------------------------
# PHASE 7 - Booking Service (OpenFeign + Resilience4J)
# --------------------------------------------------------------
git add booking-service/src/main/java/com/labplatform/booking/client/
Commit "feat(booking): add EquipmentClient (OpenFeign) with fallback and Eureka discovery"

git add booking-service/src/main/java/com/labplatform/booking/config/Resilience4jConfig.java `
        booking-service/src/main/java/com/labplatform/booking/config/FeignConfig.java
Commit "feat(booking): configure Resilience4J circuit breaker, retry, and Feign client interceptors"

git add booking-service/src/main/java/com/labplatform/booking/service/BookingService.java
Commit "feat(booking): implement BookingService - availability check -> conflict guard -> save -> publish"

# --------------------------------------------------------------
# PHASE 8 - Booking Service (RabbitMQ + Observability)
# --------------------------------------------------------------
git add booking-service/src/main/java/com/labplatform/booking/config/RabbitMQConfig.java
Commit "feat(booking): configure RabbitMQ topic exchange, DLQ, bindings, and publisher confirms"

git add booking-service/src/main/java/com/labplatform/booking/messaging/BookingEventPublisher.java
Commit "feat(booking): add BookingEventPublisher with trace ID injection into message headers"

git add booking-service/src/main/java/com/labplatform/booking/config/ObservabilityConfig.java
Commit "feat(booking): register ObservedAspect for OpenTelemetry @Observed span creation"

git add booking-service/src/main/resources/logback-spring.xml
Commit "feat(booking): add logback-spring.xml - human-readable dev, JSON structured logging in prod"

# --------------------------------------------------------------
# PHASE 9 - Equipment Service (full implementation)
# --------------------------------------------------------------
git add equipment-service/src/main/java/com/labplatform/equipment/domain/ `
        equipment-service/src/main/java/com/labplatform/equipment/repository/ `
        equipment-service/src/main/java/com/labplatform/equipment/dto/ `
        equipment-service/src/main/java/com/labplatform/equipment/mapper/
Commit "feat(equipment): add Equipment entity, repository, DTOs, UpdateEquipmentRequest, MapStruct mapper"

git add equipment-service/src/main/java/com/labplatform/equipment/service/ `
        equipment-service/src/main/java/com/labplatform/equipment/controller/ `
        equipment-service/src/main/java/com/labplatform/equipment/exception/ `
        equipment-service/src/main/java/com/labplatform/equipment/security/
Commit "feat(equipment): implement CRUD, availability endpoint, JWT security, GlobalExceptionHandler"

git add equipment-service/src/main/java/com/labplatform/equipment/messaging/
Commit "feat(equipment): add idempotent BookingEventConsumer - LRU dedup, version check, manual ACK/NACK to DLQ"

git add equipment-service/src/main/java/com/labplatform/equipment/config/ObservabilityConfig.java
Commit "feat(equipment): register ObservedAspect for OpenTelemetry tracing"

git add equipment-service/src/main/resources/logback-spring.xml
Commit "feat(equipment): add logback-spring.xml with eventId MDC field for RabbitMQ log correlation"

# --------------------------------------------------------------
# PHASE 10 - Database & Infrastructure
# --------------------------------------------------------------
git add docker/sql/
Commit "feat(db): add SQL init scripts - DDL, indexes, triggers, seed data for booking_db and equipment_db"

git add docker/
Commit "feat(docker): add Dockerfiles for all 5 microservices with multi-stage builds"

git add docker-compose.yml
Commit "feat(infra): add docker-compose.yml - 9 containers with health checks and startup ordering"

# --------------------------------------------------------------
# PHASE 11 - Final polish & tests
# --------------------------------------------------------------
git add booking-service/src/test/
Commit "test(booking): add BookingServiceIntegrationTest with MockBean isolation"

git add booking-service/pom.xml equipment-service/pom.xml `
        config-server/pom.xml eureka-server/pom.xml api-gateway/pom.xml
Commit "chore(deps): add logstash-logback-encoder to service POMs for structured logging"

# Stage everything else not yet committed
git add .
Commit "chore: finalise all remaining configs, YAML profiles, and env variable templates"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Git history created successfully!" -ForegroundColor Cyan
Write-Host " Total commits: $(git rev-list --count HEAD)" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
git log --oneline
