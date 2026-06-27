# 🔍 System Resolution & Analysis Report
**University Lab Equipment Booking & Maintenance Platform**

This document provides a comprehensive analysis of the microservices platform, mapping its features directly to the assignment rubric, detailing its internal workings, and identifying areas for production hardening.

---

## Part 1: Fulfillment of the 9 Rubric Deliverables

This project strictly adheres to the 100-point rubric for the Microservices Architecture assignment. 

### 1. Microservices Architecture Context & Domain
* **Requirement**: Spring-based distributed system, service autonomy, inter-service communication, resilience, asynchronous messaging, and end-to-end observability. Domain with two related entities.
* **Implementation**: Built with Spring Boot 3.3 and Spring Cloud 2023. The domain consists of two autonomous entities: `Booking` (managed by `booking-service`) and `Equipment` (managed by `equipment-service`), each backed by a dedicated PostgreSQL database to ensure absolute data autonomy.

### 2. REST API
* **Requirement**: Clear RESTful API endpoints for the domain entities.
* **Implementation**: Both services expose REST controllers (`BookingController`, `EquipmentController`) with standard CRUD operations (GET, POST, PUT, DELETE) using semantic HTTP status codes.

### 3. Gateway, Discovery, and Configuration
* **Requirement**: API Gateway, Eureka Service Discovery, and Spring Cloud Config Server.
* **Implementation**: 
  * **Config Server**: Centralizes configurations with `dev` and `prod` profiles. Sensitive variables (like DB passwords) are externalized via environment variables.
  * **Eureka Server**: Facilitates dynamic service registration and discovery.
  * **API Gateway**: Acts as a reverse proxy, routing client requests dynamically (e.g., `lb://booking-service`).

### 4. Security
* **Requirement**: Gateway-level security (e.g., JWT validation) to prevent unauthorized access.
* **Implementation**: `booking-service` issues JWTs upon login. The API Gateway intercepts requests using `JwtAuthenticationFilter`, validates the token signature, and forwards the identity downstream via `X-Auth-Username` and `X-Auth-Role` headers. Downstream services trust these gateway-validated headers.

### 5. Inter-Service Communication & Resilience
* **Requirement**: Inter-service communication with fault tolerance (circuit breakers/fallbacks).
* **Implementation**: `booking-service` uses OpenFeign (`EquipmentClient`) to check equipment availability synchronously. This call is wrapped with **Resilience4J** for Circuit Breaking and Fallback handling, ensuring that if `equipment-service` is down, a default response prevents cascading failures.

### 6. Asynchronous Messaging
* **Requirement**: Event-driven communication using a message broker.
* **Implementation**: When a booking is confirmed, `booking-service` publishes a `BookingCreatedEvent` to RabbitMQ (`booking.events` topic exchange). The `equipment-service` consumes this message asynchronously to increment usage counts and trigger maintenance, demonstrating eventual consistency.

### 7. Observability
* **Requirement**: Distributed tracing and logging.
* **Implementation**: The system integrates **OpenTelemetry** and **Zipkin**. A `CorrelationIdFilter` at the Gateway ensures every request receives an `X-Correlation-Id` header, which propagates synchronously via Feign and asynchronously via RabbitMQ headers. All logs and Zipkin spans are tied together across the distributed system.

### 8. Architecture Decision Records (ADRs)
* **Requirement**: Documentation of significant architectural decisions.
* **Implementation**: Three key ADRs are documented:
  * **ADR-001**: Database-per-service pattern for autonomy.
  * **ADR-002**: RabbitMQ for asynchronous booking events to ensure high availability.
  * **ADR-003**: API Gateway pattern for centralized JWT validation.

### 9. Repository Quality & Build
* **Requirement**: High-quality Git repository demonstrating a logical development process.
* **Implementation**: The repository contains a comprehensive git history (38+ commits) documenting the evolution from initial scaffolding to individual services, integration, and final dockerization.

---

## Part 2: Code Walkthrough - What the Code is Doing

The platform operates on a resilient, event-driven architecture. Here is the lifecycle of a client interaction:

### 2.1: Authentication Flow
1. **User Login**: A client submits credentials to `POST /api/auth/login`.
2. **Token Issuance**: The `booking-service` validates credentials against an `InMemoryUserDetailsManager` and generates a signed HS256 JWT containing the user's role.
3. **Subsequent Calls**: The client attaches this token to the `Authorization: Bearer <token>` header for all future requests.

### 2.2: API Gateway Routing & Filters
All client requests hit the `api-gateway`:
* **Trace Propagation**: If no `X-Correlation-Id` exists, the Gateway generates a UUID and injects it into the request headers for distributed tracing.
* **Authentication**: The Gateway validates the JWT, extracts the user details, and propagates them to downstream services via `X-Auth-Username` and `X-Auth-Role` headers.
* **Routing**: Requests are load-balanced and routed dynamically using Eureka.

### 2.3: Creating a Booking
When a user requests `POST /api/bookings`:
1. **Synchronous Check**: `booking-service` calls `equipment-service` synchronously via OpenFeign (`GET /api/equipment/{id}/availability`) to ensure the item is not decommissioned.
2. **Validation**: It queries its own `booking_db` to ensure no overlapping time slots.
3. **Creation & Event Publication**: The booking is saved as `CONFIRMED`. A `BookingCreatedEvent` is published to RabbitMQ.
4. **Asynchronous Consumption**: `equipment-service` consumes the event, increments the equipment's `usageCount`, and if the threshold is met, automatically flags the equipment for `MAINTENANCE`.

---

## Part 3: Vulnerability Assessment - What Could Break and Why?

Several architectural aspects could introduce instability in a real-world scenario:

### 3.1: State Desynchronization (Split-Brain)
* **What breaks**: Two students successfully book the same equipment for back-to-back slots, bypassing the maintenance lock.
* **Why**: The availability check is synchronous, but the maintenance increment is asynchronous via RabbitMQ. If Student B checks availability before the `BookingEventConsumer` has processed Student A's booking event, the check returns `AVAILABLE`.
* **Risk**: Medium. It violates the business rule that equipment must be serviced immediately after its 5th booking.

### 3.2: Single Points of Failure (SPOFs)
* **What breaks**: All requests fail with Gateway errors, or services refuse to start.
* **Why**: 
  * The **API Gateway** is a single point of entry. A crash here brings down the entire system.
  * The **Config Server** is loaded synchronously at startup. If it is unavailable, microservices cannot read databases, queues, or security configurations and will crash immediately.

### 3.3: Docker Startup Race Conditions
* **What breaks**: Containers start but continuously throw connection refused exceptions and fail their health checks.
* **Why**: The startup order is strictly defined, but if database services (`postgres-booking`, `postgres-equipment`) take too long to initialize their DDL scripts, the dependent microservices might timeout waiting for JPA validation and fail to start.

### 3.4: Local Compilation Incompatibility
* **What breaks**: Running `mvn clean compile` locally on Java 22+ throws `ExceptionInInitializerError`.
* **Why**: Lombok (version 1.18.34) relies on internal compiler APIs (`sun.tools.javac`) that were removed in JDK 22+. 
* **Risk**: High for local development (though mitigated by the Dockerfile which forces a JDK 21 environment).

---

## Part 4: Missing Settings - Production Dependencies Not Yet Setup

Before this architecture can be deployed to staging or production, the following infrastructure dependencies and configurations must be addressed:

### 4.1: Missing Redis Container for Rate Limiting (Critical)
* **What is missing**: There is no Redis service defined in `docker-compose.yml`.
* **Why it breaks**: The `prod` profile in the Config Server specifies a `RequestRateLimiter` filter for the Gateway, which depends on Redis. Starting the Gateway in production mode will cause it to crash.
* **Fix needed**: Add a Redis container to Docker Compose and configure `spring.data.redis.host`.

### 4.2: Production Config Server Backend
* **What is missing**: The Config Server uses the `native` profile, loading configuration from the local classpath.
* **Why it is a problem**: Updating configurations requires rebuilding the container.
* **Fix needed**: Change the active profile to `git` or `vault` to point to a secure, external configuration repository.

### 4.3: Configuration Decryption (Plaintext Secrets)
* **What is missing**: Database and RabbitMQ passwords are passed as plaintext environment variables.
* **Why it is a problem**: Server logs or Docker configs expose these secrets.
* **Fix needed**: Enable Spring Cloud Config Encryption (`{cipher}...`) so secrets remain encrypted in files and are only decrypted in-memory.

### 4.4: HTTPS / SSL (Transport Layer Security)
* **What is missing**: All communication is via plaintext HTTP.
* **Why it is a problem**: JWT tokens can be intercepted via packet sniffing (man-in-the-middle).
* **Fix needed**: Configure the Gateway to use SSL/TLS (HTTPS) for external traffic, and mTLS for internal service-to-service communication.

### 4.5: User Authentication Database
* **What is missing**: Users are hardcoded using `InMemoryUserDetailsManager`.
* **Why it is a problem**: Dynamic user registration or password changes are impossible without recompiling.
* **Fix needed**: Integrate a JPA `UserDetailsService` or an external Identity Provider (Keycloak/Auth0) using Spring Security OAuth2.

### 4.6: Log and Trace Persistent Storage
* **What is missing**: Zipkin stores traces in-memory, and JSON logs are only output to the console.
* **Fix needed**: Export Zipkin traces to Elasticsearch, and set up a log shipper (Filebeat/FluentBit) to index logs centrally in ELK or Grafana Loki.
