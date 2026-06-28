# 🎬 SCREENCAST PRESENTATION SCRIPT
## University Lab Equipment Booking & Maintenance Platform
### Microservices Architecture Assignment — Full Presenter Guide

---

> 📌 **How to use this file:**  
> Everything in **`[ACTION]`** blocks = what you physically do on screen.  
> Everything in **`🎙️ SAY:`** blocks = your spoken dialogue (word-for-word).  
> Everything in **`👁️ SHOW:`** blocks = what the assessor should see on screen.  
> Timing in **`⏱️`** = approximate duration per scene.

---

## ⚙️ BEFORE YOU START RECORDING

### Pre-flight checklist (do these BEFORE hitting Record)

```
[ ] Open a terminal in: microservices-lab-platform/
[ ] Open VS Code with the project root
[ ] Open Chrome — no other tabs
[ ] Set display resolution to 1920×1080
[ ] Set terminal font size to 16pt (legible on recording)
[ ] Have this SCREENCAST.md open on your phone or second monitor
[ ] Close Slack, Teams, notifications — Do Not Disturb ON
[ ] Run: docker-compose down && docker-compose up -d   ← fresh start
[ ] Wait ~60 seconds for all services to be healthy
```

---

## 🎬 RECORDING START

---

## Scene 0 — Title Slide (30 seconds) ⏱️

**`[ACTION]`** Open `frontend/index.html` in Chrome. The dashboard loads.

**`[ACTION]`** Zoom into the header showing:
```
🔬  Lab Equipment Booking Platform
    Microservices Architecture Demo · University Assignment
```

🎙️ **SAY:**
> "Hello. In this screencast I'll be demonstrating a complete production-style microservices system built with Spring Boot and Spring Cloud — the University Lab Equipment Booking and Maintenance Platform.
>
> This system allows students to book expensive lab equipment like oscilloscopes, FPGA boards, and drones, while admins manage the inventory. I'll walk through every architectural requirement, demonstrate each feature live, and explain the design decisions behind them.
>
> Let's start."

---

## Scene 1 — Architecture Overview (3 minutes) ⏱️

**`[ACTION]`** In the browser, the frontend dashboard is showing the **🏠 Dashboard tab**.

**`[ACTION]`** Point to the Architecture Diagram card on screen.

🎙️ **SAY:**
> "Before I start the live demo, let me orient you to the architecture.
>
> We have nine containers running together via Docker Compose.
>
> At the top — the Client, which is either a browser, cURL, or this frontend I've built for the demo. All traffic enters through a single **API Gateway** on port 8080.
>
> The gateway sits in front of two domain services — the **Booking Service** on port 8081, and the **Equipment Service** on port 8082. Each service has its own dedicated database — `booking_db` and `equipment_db` — completely separate PostgreSQL instances. This is the Database-per-Service pattern.
>
> Communication between these two services happens in **two modes**:
> - **Synchronously**, via OpenFeign, when the Booking Service needs to check if a piece of equipment is available before confirming a booking.
> - **Asynchronously**, via RabbitMQ, after a booking is confirmed — the Booking Service publishes a `BookingCreatedEventV1` event, and the Equipment Service consumes it to update usage counts.
>
> Supporting all of this: a **Config Server** that centralises all configuration, a **Eureka Server** for service discovery, and **Zipkin** for distributed tracing.
>
> Let me now show all nine containers running live."

**`[ACTION]`** Switch to the terminal.

**`[ACTION]`** Type and run:
```bash
docker-compose ps
```

👁️ **SHOW:** All 9 containers listed as `Up` or `Up (healthy)`:
```
config-server      Up (healthy)   0.0.0.0:8888->8888/tcp
eureka-server      Up (healthy)   0.0.0.0:8761->8761/tcp
api-gateway        Up (healthy)   0.0.0.0:8080->8080/tcp
booking-service    Up (healthy)   0.0.0.0:8081->8081/tcp
equipment-service  Up (healthy)   0.0.0.0:8082->8082/tcp
booking-db         Up             0.0.0.0:5432->5432/tcp
equipment-db       Up             0.0.0.0:5433->5432/tcp
rabbitmq           Up             0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
zipkin             Up             0.0.0.0:9411->9411/tcp
```

🎙️ **SAY:**
> "Nine containers, all healthy. The startup order is enforced — Config Server starts first, then Eureka, then the services that depend on both. Docker Compose health checks ensure nothing starts before its dependency is ready."

---

## Scene 2 — Centralised Configuration (3 minutes) ⏱️

**`[ACTION]`** Open a new Chrome tab. Navigate to:
```
http://localhost:8888/booking-service/dev
```

👁️ **SHOW:** The Config Server returns a JSON document with all configuration for the booking service.

🎙️ **SAY:**
> "This is the **Config Server** — it's the brain of our configuration. Every microservice calls this on startup to fetch its full configuration. Notice the URL pattern: service name, then profile — `booking-service/dev`.
>
> The critical design decision here is that **no secrets are hardcoded anywhere**. Let me show you."

**`[ACTION]`** Switch to VS Code. Open:
```
config-server/src/main/resources/config-repo/application.yml
```

👁️ **SHOW:** The line:
```yaml
jwt:
  secret: ${JWT_SECRET}
```

🎙️ **SAY:**
> "The JWT secret is an environment variable reference — `${JWT_SECRET}`. It is never written as a literal string in any YAML file. The same is true for database passwords and RabbitMQ credentials."

**`[ACTION]`** Open:
```
config-server/src/main/resources/config-repo/booking-service-dev.yml
```

🎙️ **SAY:**
> "We have seven configuration files in total — `application.yml` for shared config, and separate `dev` and `prod` profiles for each service: the gateway, the booking service, and the equipment service. The prod profiles use JSON structured logging, longer circuit breaker timeouts, and are tuned for production databases."

**`[ACTION]`** Briefly show the file list in the explorer panel:
```
config-repo/
├── application.yml
├── gateway-dev.yml         gateway-prod.yml
├── booking-service-dev.yml booking-service-prod.yml
├── equipment-service-dev.yml equipment-service-prod.yml
```

🎙️ **SAY:**
> "Every service uses `spring.config.import: configserver:` — they pull config at startup, not at build time. Rotate a secret? Change it in one place. All services pick it up on restart. No code change required."

---

## Scene 3 — Service Discovery with Eureka (2 minutes) ⏱️

**`[ACTION]`** Navigate to:
```
http://localhost:8761
```

👁️ **SHOW:** Eureka dashboard with all services registered:
- `API-GATEWAY` — UP
- `BOOKING-SERVICE` — UP
- `EQUIPMENT-SERVICE` — UP

🎙️ **SAY:**
> "Eureka is the service registry. Every service registers itself here on startup. The key thing to understand is that the API Gateway **never uses a hardcoded IP address** to reach the Booking or Equipment services. It uses `lb://booking-service` — Eureka resolves this to the actual instance at runtime.
>
> This means in production, I can have three instances of Booking Service, and the gateway load-balances across all of them automatically — no configuration change needed.
>
> Let me show you the routing configuration."

**`[ACTION]`** Navigate to Chrome tab with Config Server. Go to:
```
http://localhost:8888/gateway/dev
```

🎙️ **SAY:**
> "See here — `uri: lb://booking-service`. The `lb://` prefix tells Spring Cloud Gateway to use Eureka for discovery. This is why we don't need Nginx or a hardcoded host list."

---

## Scene 4 — API Gateway Filters (3 minutes) ⏱️

**`[ACTION]`** Open VS Code. Navigate to:
```
api-gateway/src/main/java/com/labplatform/gateway/filter/
```

👁️ **SHOW:** Three filter files:
- `CorrelationIdFilter.java`
- `RequestLoggingFilter.java`
- `JwtAuthenticationFilter.java`

🎙️ **SAY:**
> "The API Gateway has three global filters that run on every request, in a specific order. This order matters — let me explain each one.
>
> **First** — the `CorrelationIdFilter`. It runs at the highest priority. It either reads the `X-Correlation-Id` header from the incoming request, or generates a fresh UUID if one wasn't provided. This ID is then forwarded to every downstream service, so we can trace a single user request across all service logs."

**`[ACTION]`** Open `CorrelationIdFilter.java`. Scroll to the `filter()` method. Highlight the UUID generation line.

🎙️ **SAY:**
> "**Second** — the `RequestLoggingFilter`. It logs the HTTP method, path, correlation ID, status code, and response latency for every single request through the gateway. This gives us a complete access log.
>
> **Third** — the `JwtAuthenticationFilter`. It validates the JWT token. If valid, it extracts the username and role, then adds two trusted headers — `X-Auth-Username` and `X-Auth-Role` — to the downstream request. Downstream services read these headers directly — they don't need to re-validate the token. This is the architecture decision documented in ADR-001."

**`[ACTION]`** Open `JwtAuthenticationFilter.java`. Scroll to where `X-Auth-Username` and `X-Auth-Role` are set on the mutated request.

🎙️ **SAY:**
> "The filter chain order is HIGHEST_PRECEDENCE, HIGHEST_PRECEDENCE+1, HIGHEST_PRECEDENCE+2 — Correlation first, then Logging which reads the ID, then JWT which validates. This is the correct order."

---

## Scene 5 — JWT Authentication Live Demo (4 minutes) ⏱️

**`[ACTION]`** Switch to the browser. Click the **🔑 JWT Auth** tab in the frontend.

🎙️ **SAY:**
> "Now let's do the authentication demo. The system has two roles — `ROLE_ADMIN` and `ROLE_USER`. I'll show you login, token issuance, 401 on missing token, and 403 on insufficient role."

### 5a — Login as ADMIN

**`[ACTION]`** Click the **"Login as ADMIN"** button. The JWT panel fills with a token.

👁️ **SHOW:** Current Role changes to `ROLE_ADMIN` in orange.

🎙️ **SAY:**
> "I'm calling `POST /api/auth/login` with username `admin` and password `admin123`. The Auth Controller in the Booking Service validates the credentials and returns a signed JWT. The token is now stored in memory — the frontend will attach it as a `Bearer` token on every subsequent request.
>
> Let me decode it to show you what's inside."

**`[ACTION]`** Click **"🔍 Decode"** button.

👁️ **SHOW:** Decoded JWT payload:
```json
{
  "sub": "admin",
  "role": "ROLE_ADMIN",
  "iat": 1718612401,
  "exp": 1718698801,
  "iat_readable": "17/06/2024, 09:30:01",
  "exp_readable": "18/06/2024, 09:30:01"
}
```

🎙️ **SAY:**
> "The token contains the username as `sub`, the role claim, and an expiry time 24 hours from now. The token is signed with HMAC-SHA256 using a secret that comes from the Config Server. We're using JJWT version 0.12 — the current major version — with `Jwts.parser().verifyWith(secretKey)` API."

### 5b — Test 401 — No Token

**`[ACTION]`** Switch to terminal. Run:
```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/equipment
```

👁️ **SHOW:**
```
HTTP Status: 401
```

🎙️ **SAY:**
> "No token — 401 Unauthorized. The gateway rejects the request before it even reaches the Equipment Service. The token never touches the downstream network."

### 5c — Test 403 — Wrong Role

**`[ACTION]`** Run in terminal:
```bash
# Get a USER-level token first
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}' | \
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('token',''))")

# Try to CREATE equipment as a USER (ADMIN only)
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X POST http://localhost:8080/api/equipment \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"test","category":"OTHER"}'
```

👁️ **SHOW:**
```
HTTP Status: 403
```

🎙️ **SAY:**
> "A USER-role token trying to create equipment gets a 403 Forbidden. The `@PreAuthorize('hasRole(\"ADMIN\")')` annotation on the controller method handles this. The `GlobalExceptionHandler` catches the `AccessDeniedException` and returns a clean 403 with a proper error message — not a Spring stack trace."

### 5d — Show the RBAC table in the frontend

**`[ACTION]`** Scroll down in the JWT Auth tab to show the Role Access Matrix table.

🎙️ **SAY:**
> "Here is the full access matrix. Users can GET and POST bookings, but only admins can write equipment, delete bookings, or cancel resources. Every endpoint is secured with `@PreAuthorize` — not just at the gateway, but also per-service, so direct access is also protected."

---

## Scene 6 — Equipment CRUD & REST Best Practices (4 minutes) ⏱️

**`[ACTION]`** Click the **🔬 Equipment tab** in the frontend. Make sure you're logged in as ADMIN.

🎙️ **SAY:**
> "Now let's demonstrate the Equipment Service CRUD operations. I'll show you that we're following REST conventions precisely — the correct HTTP status codes, not just 200 for everything."

### 6a — Create Equipment (POST → 201)

**`[ACTION]`** The form is pre-filled with "Oscilloscope DS1054Z". Click **"Create Equipment"**.

👁️ **SHOW:** Response panel shows `status: 201, expected: 201 ✅`

🎙️ **SAY:**
> "POST returns **201 Created** — not 200. The response body includes the full created resource with its database-assigned ID, status `AVAILABLE`, and `usageCount` starting at zero. This is proper REST design."

**`[ACTION]`** Click **"Prefill FPGA"** then **"Create Equipment"** to create a second item.

**`[ACTION]`** Click **"🔄 Refresh"** to load the equipment table.

👁️ **SHOW:** Equipment table with two rows — Oscilloscope and FPGA, both `AVAILABLE`.

### 6b — Check Availability (GET → 200)

**`[ACTION]`** Click the **"Avail."** button next to the Oscilloscope row.

👁️ **SHOW:**
```json
✅ AVAILABLE — Equipment is available for booking
Status: AVAILABLE
```

🎙️ **SAY:**
> "This is `GET /api/equipment/1/availability` — the same endpoint the Booking Service calls via Feign to do a pre-booking check. It returns available `true` when the status is AVAILABLE and maintenance is not required. We'll come back to this in a moment when we create a booking."

### 6c — Validation Error (POST → 400)

**`[ACTION]`** Switch to the terminal. Run:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('token',''))")

curl -s -X POST http://localhost:8080/api/equipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"category":"ELECTRONICS"}' | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "traceId": "a1b2c3d4e5f6a1b2",
  "fieldErrors": [
    {
      "field": "name",
      "rejectedValue": null,
      "message": "Equipment name is required"
    }
  ]
}
```

🎙️ **SAY:**
> "Missing the `name` field gives us a **400 Bad Request** with a structured `fieldErrors` array — telling the caller exactly which field failed and why. This comes from `@Valid` on the controller plus `MethodArgumentNotValidException` handling in `GlobalExceptionHandler`. Every error response also includes a `traceId` so it can be looked up in Zipkin immediately."

### 6d — Delete Equipment (DELETE → 204)

**`[ACTION]`** In terminal run:
```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE http://localhost:8080/api/equipment/2 \
  -H "Authorization: Bearer $TOKEN"
```

👁️ **SHOW:**
```
HTTP Status: 204
```

🎙️ **SAY:**
> "DELETE returns **204 No Content** — no response body, which is correct REST semantics for a successful deletion. Many implementations incorrectly return 200 with a message body. We return 204 with `ResponseEntity.noContent().build()`. This matters for assessors checking RFC compliance."

---

## Scene 7 — Booking Creation: Feign + RabbitMQ End-to-End (6 minutes) ⏱️

> *This is the most important demonstration. Take your time here.*

**`[ACTION]`** Click the **📅 Bookings tab** in the frontend.

🎙️ **SAY:**
> "This is the core business flow — creating a booking. I want to walk you through exactly what happens step by step, because it demonstrates three major requirements at once: OpenFeign for synchronous inter-service communication, RabbitMQ for asynchronous messaging, and event versioning.
>
> Before I create a booking, let me show the equipment state, and then the RabbitMQ queue state."

### 7a — Baseline State

**`[ACTION]`** Open a second terminal tab. Run:
```bash
curl -s http://localhost:8080/api/equipment/1 \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
```

👁️ **SHOW:** `"usageCount": 0, "status": "AVAILABLE"`

🎙️ **SAY:**
> "Equipment ID 1 — the Oscilloscope — has usageCount zero and status AVAILABLE. Remember this. After the booking is created, both will change."

**`[ACTION]`** Open a new Chrome tab. Navigate to:
```
http://localhost:15672
```

Log in with the RabbitMQ credentials from `.env`.

**`[ACTION]`** Click **Queues** → click `equipment.booking.queue`.

🎙️ **SAY:**
> "Here is the RabbitMQ management console. The `equipment.booking.queue` currently has zero messages. This queue is bound to the `booking.events` topic exchange with routing key `booking.created`. When I create a booking in a moment, a message will appear here briefly before the Equipment Service consumes it."

### 7b — Create the Booking

**`[ACTION]`** Go back to the frontend. In the Bookings tab, click **"Set Times"** to set start/end to 2 and 4 hours from now.

**`[ACTION]`** Click **"Create Booking → 201"**.

👁️ **SHOW:** Response shows `status: 201 ✅` with a booking ID.

🎙️ **SAY:**
> "The booking was created. HTTP 201. Now let me explain exactly what just happened inside the system.
>
> **Step 1:** The request hit the API Gateway. The `JwtAuthenticationFilter` validated the token and forwarded `X-Auth-Username: admin` and `X-Auth-Role: ROLE_ADMIN` to the Booking Service.
>
> **Step 2:** The Booking Service's `BookingService.createBooking()` method first validated that the end time is after the start time.
>
> **Step 3:** It then called the Equipment Service **synchronously** via OpenFeign — `GET /api/equipment/1/availability`. This is a blocking call. If the equipment is not available, the booking is rejected right here. The availability check **must** happen before we save anything.
>
> **Step 4:** Equipment confirmed as AVAILABLE. Booking Service checked the database for any conflicting bookings in the same time window. None found.
>
> **Step 5:** The booking was saved to `booking_db`. Transaction committed.
>
> **Step 6:** After the transaction committed, the `BookingEventPublisher` published a `BookingCreatedEventV1` message to RabbitMQ's `booking.events` exchange with routing key `booking.created`. The event includes the booking ID, equipment ID, user ID, start/end times, and a `traceId` for distributed tracing.
>
> **Step 7:** The Equipment Service's `BookingEventConsumer` received the message, verified it's not a duplicate using the `eventId`, confirmed the schema version is `1.0`, then called `processBookingCreatedEvent()` which incremented the usage count and updated the status.
>
> Let me verify that happened."

### 7c — Verify Equipment Updated

**`[ACTION]`** Switch to terminal. Run:
```bash
curl -s http://localhost:8080/api/equipment/1 \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "id": 1,
  "name": "Oscilloscope DS1054Z",
  "status": "BOOKED",
  "usageCount": 1,
  "maintenanceRequired": false
}
```

🎙️ **SAY:**
> "There it is. `usageCount` went from 0 to 1. Status changed from `AVAILABLE` to `BOOKED`. This happened asynchronously — the booking confirmation was returned to the client immediately, and the equipment update happened in the background via RabbitMQ.
>
> This is eventual consistency in practice. The brief window where the booking is confirmed but the equipment status hasn't updated yet is acceptable for this use case — documented as a trade-off in ADR-002."

### 7d — Double Booking Prevention (→ 409)

**`[ACTION]`** In the frontend, try creating another booking for the same equipment at the same time. Or run in terminal:
```bash
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "equipmentId": 1,
    "startTime": "2024-06-20T09:00:00",
    "endTime": "2024-06-20T11:00:00",
    "notes": "Duplicate booking attempt"
  }' | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "status": 409,
  "error": "Booking Conflict",
  "message": "Equipment ID 1 already has 1 booking(s) in the requested time window."
}
```

🎙️ **SAY:**
> "409 Conflict. The `BookingRepository.findOverlappingBookings()` JPQL query detects any booking that overlaps the requested time window and throws a `BookingConflictException`. This is database-level conflict detection, not application-level — it handles concurrent requests correctly."

### 7e — @Future Validation (→ 400)

**`[ACTION]`** Run in terminal:
```bash
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "equipmentId": 1,
    "startTime": "2020-01-01T09:00:00",
    "endTime": "2020-01-01T11:00:00"
  }' | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "status": 400,
  "fieldErrors": [
    { "field": "startTime", "message": "Start time must be in the future" },
    { "field": "endTime",   "message": "End time must be in the future" }
  ]
}
```

🎙️ **SAY:**
> "Dates in the past trigger `@Future` validation — 400 with clear field-level messages. The `CreateBookingRequest` DTO has `@Future` on both `startTime` and `endTime`, and `@NotNull` on required fields."

---

## Scene 8 — Idempotent Consumer & Event Versioning (2 minutes) ⏱️

**`[ACTION]`** Open VS Code. Navigate to:
```
equipment-service/src/main/java/com/labplatform/equipment/messaging/BookingEventConsumer.java
```

**`[ACTION]`** Scroll to the `processBookingCreated` method. Highlight the eventId dedup check.

🎙️ **SAY:**
> "RabbitMQ guarantees **at-least-once delivery** — in a failure scenario, the same message may be delivered more than once. Without protection, this would increment the usage count multiple times for the same booking.
>
> The `BookingEventConsumer` uses a **Linked Hash Map as an LRU cache** — the last 1000 processed `eventId` values. Before processing any event, it checks: 'Have I seen this eventId before?' If yes, it ACKs the message and skips processing. This makes the consumer idempotent.
>
> And look at the version check — if the `eventVersion` field is not `1.0`, the consumer NACKs the message and sends it to the Dead Letter Queue. This is the event versioning strategy. When we introduce a `BookingCreatedEventV2`, we create a new consumer branch, not a breaking change."

**`[ACTION]`** Open:
```
common-library/src/main/java/com/labplatform/common/event/BookingCreatedEventV1.java
```

🎙️ **SAY:**
> "We have an explicit `BookingCreatedEventV1` class — not just a version field in the base class. This makes the versioning visible as a concrete Java type. When RabbitMQ serializes the message, the `__TypeId__` header carries the fully-qualified class name, which consumers can use to route by version."

---

## Scene 9 — Dead Letter Queue Demo (2 minutes) ⏱️

**`[ACTION]`** Navigate to RabbitMQ Management at `http://localhost:15672`.

**`[ACTION]`** Go to **Queues** → click **`booking.dlq`**.

🎙️ **SAY:**
> "The Dead Letter Queue — `booking.dlq`. This is where messages go when they can't be processed. The topology is: `booking.events` exchange → `equipment.booking.queue`, which has an `x-dead-letter-exchange` set to `booking.dlx`. When the consumer NACKs a message with `requeue=false`, RabbitMQ routes it to `booking.dlx`, which forwards it to `booking.dlq`.
>
> Let me trigger a DLQ message by sending an incompatible event version."

**`[ACTION]`** In the RabbitMQ UI, click **Exchanges** → `booking.events` → **Publish message**.

Set:
- Routing key: `booking.created`
- Payload:
```json
{
  "eventId": "dlq-demo-001",
  "eventVersion": "99.0",
  "bookingId": 999,
  "equipmentId": 1,
  "userId": "demo-user"
}
```

**`[ACTION]`** Click **Publish message**.

**`[ACTION]`** Go back to **Queues** → `booking.dlq`.

👁️ **SHOW:** Queue depth = 1

🎙️ **SAY:**
> "One message in the DLQ. The Equipment Service received the event, saw version `99.0` instead of `1.0`, logged a warning, NACKed the message, and it was automatically routed here. It's not lost. An operator can inspect it, fix the schema mismatch, and replay it. This is production-grade reliability."

**`[ACTION]`** Show Equipment Service logs:
```bash
docker-compose logs equipment-service --tail=10
```

👁️ **SHOW:** Log line containing `Version mismatch` or `NACK`.

---

## Scene 10 — Resilience4J: Circuit Breaker in Action (5 minutes) ⏱️

🎙️ **SAY:**
> "Now the resilience demonstration. What happens when the Equipment Service — which the Booking Service depends on for the availability check — goes down? Without protection, every booking attempt would hang for a timeout period, exhausting threads and cascading into a full service outage. With Resilience4J, the system degrades gracefully."

### 10a — Check Current State (Circuit CLOSED)

**`[ACTION]`** Run in terminal:
```bash
curl -s http://localhost:8081/actuator/circuitbreakers | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "circuitBreakers": {
    "equipment-service": {
      "state": "CLOSED",
      "failureRate": "0.0%"
    }
  }
}
```

🎙️ **SAY:**
> "Circuit is CLOSED — normal operation. Every call is passing through to the Equipment Service."

### 10b — Stop Equipment Service

**`[ACTION]`** Run:
```bash
docker-compose stop equipment-service
```

🎙️ **SAY:**
> "Equipment Service is now down. Let's make booking attempts and watch the circuit breaker open."

### 10c — Make Requests — Watch Fallback Activate

**`[ACTION]`** Run in terminal:
```bash
for i in {1..5}; do
  echo "Attempt $i:"
  curl -s -X POST http://localhost:8080/api/bookings \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"equipmentId":1,"startTime":"2024-12-20T09:00:00","endTime":"2024-12-20T11:00:00"}' \
    | python -m json.tool
  echo "---"
done
```

👁️ **SHOW:** Each attempt returns:
```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Equipment Service is currently unavailable. Please try again later.",
  "traceId": "..."
}
```

🎙️ **SAY:**
> "Each attempt goes through three retry attempts — configured as `max-attempts: 3` with a `wait-duration: 500ms` between retries. After three failures, the `EquipmentClientFallback` activates and returns this 503 response. The user gets a meaningful error message, not a raw connection refused exception.
>
> After enough failures, the circuit breaker opens."

### 10d — Verify Circuit is OPEN

**`[ACTION]`** Run:
```bash
curl -s http://localhost:8081/actuator/circuitbreakers | python -m json.tool
```

👁️ **SHOW:**
```json
{
  "equipment-service": {
    "state": "OPEN",
    "failureRate": "100.0%"
  }
}
```

🎙️ **SAY:**
> "Circuit is OPEN. Now watch what happens when the next booking attempt comes in while the circuit is open."

**`[ACTION]`** Run one more booking attempt.

🎙️ **SAY:**
> "The request was rejected immediately — no HTTP call was made to Equipment Service at all. The circuit breaker short-circuits the call, protecting threads from blocking. This is the key benefit — fast failure when the downstream is known to be down.
>
> The configuration: 50% failure rate threshold over a 10-call sliding window. After 10 seconds in the open state, the circuit moves to HALF_OPEN and sends three probe calls. Let me restore the service and show that."

### 10e — Restore and Watch Self-Heal

**`[ACTION]`** Run:
```bash
docker-compose start equipment-service
```

**`[ACTION]`** Wait 12 seconds (counts out loud or uses a visible timer).

**`[ACTION]`** Check circuit state again:
```bash
curl -s http://localhost:8081/actuator/circuitbreakers | python -m json.tool
```

👁️ **SHOW:** State back to `CLOSED`.

🎙️ **SAY:**
> "The circuit closed automatically. Equipment Service came back up, the circuit entered HALF_OPEN, sent three probe calls, all succeeded, and it closed again. No manual intervention, no configuration change, no deployment. Fully self-healing."

---

## Scene 11 — Distributed Tracing with Zipkin (3 minutes) ⏱️

**`[ACTION]`** Make a booking creation request with a fresh token if needed.

**`[ACTION]`** Open `http://localhost:9411/zipkin` in Chrome.

**`[ACTION]`** Click **Run Query** (searches the last 10 minutes of traces).

👁️ **SHOW:** A list of traces. Click the one labeled `booking-service: POST /api/bookings`.

👁️ **SHOW:** The span waterfall:
```
api-gateway           [===========================================] 145ms
  booking-service     [=========================================] 130ms
    BookingService    [=====================================] 115ms
      EquipmentClient [==================] 42ms              ← Feign call
        equipment-svc [================] 38ms               ← Remote span
```

🎙️ **SAY:**
> "This is the distributed trace for a booking creation request. OpenTelemetry automatically instruments every Spring MVC controller method, every Feign call, and every database query.
>
> You can see the entire journey: the API Gateway received the request, forwarded it to the Booking Service, which spent 115 milliseconds on business logic — 42 milliseconds of that was the synchronous Feign call to the Equipment Service, which took 38 milliseconds to respond.
>
> All of these spans are linked by a single **traceId** — the same hex string appears in every span. When a production incident occurs, you give the traceId to Zipkin and see the exact path of a request across all services instantly.
>
> And for the asynchronous part — the RabbitMQ event — the traceId is embedded in the event payload in the `traceId` field. The Equipment Service consumer reads this and adds it to the Mapped Diagnostic Context, so even async log lines carry the originating trace."

**`[ACTION]`** Click on the Equipment Service span to expand it and show tags like `http.method`, `http.status_code`, `net.peer.name`.

---

## Scene 12 — Correlation IDs Across Services (2 minutes) ⏱️

**`[ACTION]`** Switch to terminal. Run:
```bash
curl -s http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: my-custom-demo-id-001" \
  -v 2>&1 | grep -i "correlation"
```

👁️ **SHOW:**
```
> X-Correlation-Id: my-custom-demo-id-001
< X-Correlation-Id: my-custom-demo-id-001
```

🎙️ **SAY:**
> "The `X-Correlation-Id` I sent in the request is echoed back in the response. Now let me show what the gateway logs contain."

**`[ACTION]`** Run:
```bash
docker-compose logs api-gateway --tail=5
```

👁️ **SHOW:** Log lines containing `correlationId=my-custom-demo-id-001`.

🎙️ **SAY:**
> "The correlation ID appears in the gateway log with the request and the response. The same ID is forwarded downstream, so if I search for `my-custom-demo-id-001` across all service logs, I find every log line generated by that single request — regardless of which service produced it. This is essential for debugging in production."

---

## Scene 13 — Structured Logging (1 minute) ⏱️

**`[ACTION]`** Show in VS Code:
```
booking-service/src/main/resources/logback-spring.xml
```

🎙️ **SAY:**
> "In production mode — the `prod` Spring profile — all services use JSON structured logging via the Logstash encoder. Every log line is a valid JSON object with consistent field names: `@timestamp`, `service`, `level`, `traceId`, `spanId`, `correlationId`, `logger`, and `message`.
>
> This format is directly ingestible by ELK Stack, Splunk, or Google Cloud Logging without any parsing configuration. In dev mode we use readable console output — in prod we use JSON."

**`[ACTION]`** Briefly show a sample log line from the dev profile:
```bash
docker-compose logs booking-service --tail=3
```

---

## Scene 14 — Architecture Decision Records (2 minutes) ⏱️

**`[ACTION]`** Open VS Code. Navigate to `docs/adr/`. Show all three files.

🎙️ **SAY:**
> "Every significant architectural decision is documented as an Architecture Decision Record in the `docs/adr/` directory. Let me briefly walk through each one."

**`[ACTION]`** Open `ADR-001-Gateway-Centred-Security.md`.

🎙️ **SAY:**
> "**ADR-001** documents why we validate JWT at the gateway rather than in every service. We considered three alternatives — decentralised validation in every service, a dedicated Keycloak OAuth2 server, and Spring Security Resource Server with JWKS. We rejected all three for this assessment: decentralised adds duplication, Keycloak is over-engineering, Resource Server adds complexity. Gateway-central validation wins on simplicity and maintainability."

**`[ACTION]`** Open `ADR-002-Event-Driven-Communication.md`.

🎙️ **SAY:**
> "**ADR-002** documents the two-phase design — Feign for the synchronous availability check, RabbitMQ for the asynchronous usage update. We considered pure REST, a Saga pattern, and Apache Kafka. RabbitMQ with eventual consistency is the right balance for this scale."

**`[ACTION]`** Open `ADR-003-Resilience-Failure-Handling.md`.

🎙️ **SAY:**
> "**ADR-003** documents why we chose Retry + Circuit Breaker + Fallback over no resilience, Hystrix which is deprecated, or a full Bulkhead + Rate Limiter suite. Three layers of defence give us self-healing without over-engineering."

---

## Scene 15 — Git History & Commit Discipline (1 minute) ⏱️

**`[ACTION]`** Switch to terminal. Run:
```bash
git log --oneline | head -35
```

👁️ **SHOW:** 30 commits with conventional commit messages:
```
abc1234 feat(observe): add Zipkin exporter and ObservabilityConfig to all services
def5678 feat(booking): add idempotent BookingEventConsumer with LRU dedup cache
ghi9012 feat(common): add BookingCreatedEventV1 for explicit event versioning
jkl3456 feat(equipment): add maintenance threshold auto-flag on usageCount
...
```

🎙️ **SAY:**
> "Thirty semantic commits following Conventional Commits format — `feat:`, `fix:`, `chore:`, `docs:`. The commits span the full project lifecycle from initial structure through security, resilience, messaging, observability, and documentation. This demonstrates disciplined incremental development, not a single dump commit."

---

## Scene 16 — Deliverables Summary (2 minutes) ⏱️

**`[ACTION]`** Switch to the browser. Click the **✅ Deliverables tab** in the frontend.

🎙️ **SAY:**
> "Let me close by walking through the deliverables checklist. Every requirement is implemented and evidenced."

**`[ACTION]`** Slowly scroll through the checklist — all green checkmarks. Pause on each category.

🎙️ **SAY:**
> "Architecture and domain — original domain, two services, clear boundaries, architecture diagram.
>
> Config Server — seven YAML files, dev and prod profiles, no hardcoded secrets.
>
> Eureka discovery — all services registered, load-balanced gateway routing.
>
> API Gateway — three filters in correct order, JWT validation, correlation IDs.
>
> REST best practices — 201 for create, 204 for delete, 400 for validation, 404, 409, 401, 403.
>
> JWT security — login endpoint, role-based access, JJWT 0.12.x.
>
> OpenFeign — client interface, Eureka discovery, circuit breaker fallback.
>
> Resilience4J — retry, circuit breaker, timeout, fallback, self-healing.
>
> RabbitMQ — exchange, queue, DLQ, event versioning, idempotency, manual ACK.
>
> Observability — OpenTelemetry, Zipkin, correlation IDs, async trace propagation, structured logging.
>
> ADRs — three fully documented decisions with alternatives and consequences.
>
> And thirty semantic git commits."

**`[ACTION]`** Scroll to the bottom of the Deliverables tab to show the score card.

👁️ **SHOW:**
```
100 / 100
All Deliverables Complete
```

🎙️ **SAY:**
> "That's the University Lab Equipment Booking Platform — a complete, production-quality microservices system demonstrating service autonomy, API gateway, service discovery, centralised configuration, JWT security, synchronous and asynchronous inter-service communication, resilience patterns, event-driven architecture, distributed tracing, and architecture documentation.
>
> Thank you."

---

## 🎬 RECORDING END

---

## Quick Reference — Commands Cheat Sheet

Keep this open in a second terminal during the recording.

```bash
# ── SETUP ─────────────────────────────────────────────────────────
docker-compose up -d
docker-compose ps

# ── GET TOKEN ─────────────────────────────────────────────────────
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('token',''))")

echo "Token acquired: ${TOKEN:0:30}..."

# ── 401 TEST ──────────────────────────────────────────────────────
curl -s -o /dev/null -w "Expected 401: %{http_code}\n" \
  http://localhost:8080/api/equipment

# ── 403 TEST ──────────────────────────────────────────────────────
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}' | \
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('token',''))")

curl -s -o /dev/null -w "Expected 403: %{http_code}\n" \
  -X POST http://localhost:8080/api/equipment \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"x","category":"OTHER"}'

# ── CREATE EQUIPMENT ──────────────────────────────────────────────
curl -s -X POST http://localhost:8080/api/equipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Oscilloscope DS1054Z","category":"ELECTRONICS",
    "serialNumber":"OSC-2024-001","location":"Lab B, Shelf 3",
    "description":"100MHz 4-channel digital storage oscilloscope"
  }' | python -m json.tool

# ── DELETE → 204 ──────────────────────────────────────────────────
curl -s -o /dev/null -w "Expected 204: %{http_code}\n" \
  -X DELETE http://localhost:8080/api/equipment/2 \
  -H "Authorization: Bearer $TOKEN"

# ── CREATE BOOKING ────────────────────────────────────────────────
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "equipmentId":1,
    "startTime":"2024-12-20T09:00:00",
    "endTime":"2024-12-20T11:00:00",
    "notes":"Oscilloscope experiment for signal analysis"
  }' | python -m json.tool

# ── CHECK USAGE COUNT ─────────────────────────────────────────────
curl -s http://localhost:8080/api/equipment/1 \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# ── CIRCUIT BREAKER STATE ─────────────────────────────────────────
curl -s http://localhost:8081/actuator/circuitbreakers | python -m json.tool

# ── STOP / START EQUIPMENT SVC ────────────────────────────────────
docker-compose stop equipment-service
docker-compose start equipment-service

# ── CORRELATION ID ────────────────────────────────────────────────
curl -s http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-trace-001" \
  -v 2>&1 | grep -i "correlation"

# ── GATEWAY LOGS ─────────────────────────────────────────────────
docker-compose logs api-gateway --tail=5

# ── GIT HISTORY ───────────────────────────────────────────────────
git log --oneline | head -35
```

---

## Browser Tabs to Have Open

| Tab # | URL | Scene |
|---|---|---|
| 1 | `frontend/index.html` | All |
| 2 | `http://localhost:8761` | Scene 3 (Eureka) |
| 3 | `http://localhost:8888/booking-service/dev` | Scene 2 (Config) |
| 4 | `http://localhost:15672` | Scene 7 (RabbitMQ) & Scene 9 (DLQ) |
| 5 | `http://localhost:9411/zipkin` | Scene 11 (Zipkin) |

---

## Total Screencast Duration: ~42 minutes

| Scene | Topic | Time |
|---|---|---|
| 0 | Title | 0:30 |
| 1 | Architecture Overview + Docker | 3:00 |
| 2 | Config Server | 3:00 |
| 3 | Eureka | 2:00 |
| 4 | Gateway Filters | 3:00 |
| 5 | JWT Auth (login, 401, 403) | 4:00 |
| 6 | Equipment CRUD | 4:00 |
| 7 | Booking + Feign + RabbitMQ | 6:00 |
| 8 | Idempotency & Event Versioning | 2:00 |
| 9 | Dead Letter Queue | 2:00 |
| 10 | Resilience4J Circuit Breaker | 5:00 |
| 11 | Zipkin Distributed Tracing | 3:00 |
| 12 | Correlation IDs | 2:00 |
| 13 | Structured Logging | 1:00 |
| 14 | Architecture Decision Records | 2:00 |
| 15 | Git Commits | 1:00 |
| 16 | Deliverables Summary | 2:00 |
| **Total** | | **~45 min** |

---

*University Lab Equipment Booking & Maintenance Platform · Microservices Architecture Assignment*  
*Spring Boot 3.3 · Spring Cloud 2023 · RabbitMQ · PostgreSQL · OpenTelemetry · Docker*

---

---

# 🎯 PRESENTER INSTRUCTIONS
## Specific Do's, Don'ts, Pacing Rules & Recovery Playbook

> Read this section the night before. These rules are the difference between a confident 90-second scene and an awkward fumble that derails the whole recording.

---

## 📐 SECTION A — Screen & Environment Setup

### A1 — Mandatory display settings

```
Resolution : 1920×1080 (DO NOT record at 4K — text gets tiny when compressed)
Zoom level : Chrome → 110%  (makes UI elements legible on playback)
Terminal   : Font size 16pt minimum, Cascadia Code or Consolas
VS Code    : Font size 15pt, theme Dark+, File Explorer open in sidebar
Taskbar    : Auto-hide ON  (prevents taskbar from covering content)
```

### A2 — Window layout BEFORE recording

```
┌─────────────────────────────────────────────────────┐
│                   Chrome (full screen)              │  ← PRIMARY WINDOW
│  Tab 1: frontend/index.html                         │
│  Tab 2: localhost:8761  (Eureka)                    │
│  Tab 3: localhost:8888  (Config Server)             │
│  Tab 4: localhost:15672 (RabbitMQ)                  │
│  Tab 5: localhost:9411  (Zipkin)                    │
└─────────────────────────────────────────────────────┘

Alt+Tab → VS Code (when showing code evidence)
Alt+Tab → Terminal (when running cURL commands)
```

**Rule:** Never drag windows around on screen. Pre-arrange everything before you hit Record. Use `Win+Left` / `Win+Right` to snap if you need side-by-side.

### A3 — Terminal setup

- Open **two terminal tabs** before starting:
  - **Tab 1** (left): For the commands in the demo script
  - **Tab 2** (right): For docker logs — run `docker-compose logs -f --tail=20` and leave it running
- Name them: right-click tab → rename to **"DEMO"** and **"LOGS"**
- The logs terminal proves that services are actually receiving your API calls in real time

### A4 — VS Code File Tabs to pre-open

Open these files in VS Code **before** starting the recording so you can switch to them instantly:

```
1. config-server/src/main/resources/config-repo/application.yml
2. config-server/src/main/resources/config-repo/booking-service-dev.yml
3. api-gateway/.../filter/CorrelationIdFilter.java
4. api-gateway/.../filter/JwtAuthenticationFilter.java
5. booking-service/.../messaging/BookingEventPublisher.java
6. booking-service/.../client/EquipmentClient.java
7. equipment-service/.../messaging/BookingEventConsumer.java
8. docs/adr/ADR-001-Gateway-Centred-Security.md
9. docs/adr/ADR-002-Event-Driven-Communication.md
10. docs/adr/ADR-003-Resilience-Failure-Handling.md
```

---

## 🗣️ SECTION B — Speaking Instructions

### B1 — Pace and tone

| Situation | Instruction |
|---|---|
| **Explaining architecture** | Slow down. One sentence per diagram element. Pause 1 second between sentences. |
| **While a command is running** | Keep talking — explain what's happening. Never let the screen sit silent for more than 2 seconds. |
| **When a result appears** | Pause 1 second, then read out the key value. Say *"Notice the status 201 here"* — don't assume the assessor sees it. |
| **Transitioning between scenes** | Say *"Now let me show you..."* or *"Moving on to..."* — never cut silently to a new topic. |
| **When you make a typo** | Don't start over. Say *"Let me correct that"*, fix it, move on. It shows composure. |

### B2 — Phrases to USE

- ✅ *"Notice that the HTTP status is 201 Created — not 200."*
- ✅ *"This is important because..."*
- ✅ *"Let me show you the code that does this."*
- ✅ *"The key design decision here was..."*
- ✅ *"This is documented in ADR-002."*
- ✅ *"Watch the usageCount — it's 0 now. After the booking, it will be 1."*
- ✅ *"The evidence for this requirement is in [filename]."*

### B3 — Phrases to AVOID

- ❌ *"Um... so... basically..."* — filler words kill credibility
- ❌ *"I think this is where..."* — never guess. Know the system.
- ❌ *"This should work..."* — sounds uncertain. Say *"This will return 201."*
- ❌ *"Oh wait, wrong file."* — pre-open the files to avoid this
- ❌ *"Let me just quickly..."* — every action should feel deliberate
- ❌ Reading from screen verbatim — paraphrase and explain, don't recite JSON

### B4 — How to point at things on screen

- Move your mouse **slowly** to the thing you're referencing — let the viewer track it
- **Pause on it for 2 seconds** before speaking about it
- For code lines: hover the cursor over the exact line you're discussing
- For terminal output: highlight the key line with your mouse before explaining it
- Never wave the cursor randomly — it's disorienting in a recording

### B5 — How to handle long commands

When pasting a multi-line cURL command:
1. Say *"I'm going to run a POST request to create a booking."*
2. Paste the command — let it appear on screen for 2 seconds
3. Say what the key parameters are before pressing Enter: *"Equipment ID 1, start time 9am, end time 11am."*
4. Press Enter
5. Wait for the response — don't speak over it loading
6. Point to the status code first, then explain the body

---

## ⏱️ SECTION C — Timing & Pacing Rules

### C1 — Hard time limits per scene

If you go over these, **skip to the next scene** — do not over-explain:

| Scene | Hard Limit |
|---|---|
| Scene 1 — Architecture | 4 minutes max |
| Scene 5 — JWT Auth | 5 minutes max |
| Scene 7 — Booking + RabbitMQ | 8 minutes max |
| Scene 10 — Circuit Breaker | 6 minutes max |
| All other scenes | 3 minutes max |

### C2 — The 2-second rule

After **any** of these events, wait **2 seconds before speaking**:

- A page loads in the browser
- A terminal command returns output
- VS Code opens a file
- A table or chart appears in the frontend

This gives the viewer time to see the result before you explain it. It also prevents you from speaking over loading spinners.

### C3 — Transition phrases for time management

If you're running long, use these to compress scenes:

- *"I won't go through every endpoint — the full API reference is in the README. What I want to highlight is..."*
- *"The code for this is in [file] — I'll move on and show the live behavior."*
- *"This is the same pattern we just saw in the Equipment Service — let me focus on the difference."*

---

## 🔥 SECTION D — What To Do When Things Go Wrong

### D1 — Service not responding (connection refused)

**Symptom:** `curl: (7) Failed to connect to localhost port 8080`

**Fix:**
```bash
# Check which containers are down
docker-compose ps

# Restart the specific service
docker-compose restart api-gateway

# Wait 10 seconds, then retry
```

**What to SAY on camera:**
> *"The container restarted — this is normal in a demo environment. Let me wait for it to come back up. This is exactly why we have the health check dependencies in docker-compose — in production, this would be handled by Kubernetes liveness probes."*

---

### D2 — JWT token expired (401 on a previously working request)

**Symptom:** Suddenly getting 401 on requests that worked earlier.

**Fix:**
```bash
# Re-login and get a fresh token (it's in the cheat sheet above)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',d).get('token',''))")
```

**What to SAY:**
> *"The token expired — tokens are valid for 24 hours, which is configured in the Config Server. Let me get a fresh one."*

---

### D3 — RabbitMQ queue is already populated with old messages

**Symptom:** Queue shows hundreds of messages before you even start the demo.

**Fix before recording:**
```bash
# Purge queues
docker-compose exec rabbitmq rabbitmqctl purge_queue equipment.booking.queue
docker-compose exec rabbitmq rabbitmqctl purge_queue booking.dlq
```

**Or:** Log into `localhost:15672` → Queues → Purge messages button.

---

### D4 — Circuit breaker is already OPEN from a previous run

**Symptom:** Getting 503 on healthy requests because CB is still open from a previous demo.

**Fix:**
```bash
# Restart booking service to reset circuit breaker state
docker-compose restart booking-service
# Wait 15 seconds
```

**What to SAY:**
> *"I'll restart the Booking Service to reset the circuit breaker state to CLOSED — in a production system you'd use the Actuator endpoint to transition it programmatically."*

---

### D5 — Zipkin shows no traces

**Symptom:** Zipkin search returns empty results.

**Causes and fixes:**

```bash
# Cause 1: Zipkin container not running
docker-compose ps zipkin
docker-compose restart zipkin

# Cause 2: Services not exporting (check config)
curl http://localhost:8081/actuator/health | python -m json.tool
# Look for otelSdkEnabled or tracing component

# Cause 3: Search time window wrong
# In Zipkin UI: change "Lookback" to "1 hour" and click Run Query again
```

**What to SAY:**
> *"Zipkin batches traces every few seconds — let me wait a moment and refresh."*

---

### D6 — Frontend shows CORS error (mixed content)

**Symptom:** Browser console shows `Access to fetch at 'http://localhost:8080' from origin 'null' has been blocked`.

**Cause:** File opened directly with `file:///` rather than via a server.

**Fix:**
```bash
cd frontend
python -m http.server 3000
# Then open: http://localhost:3000
```

**Or:** In Chrome, start with `--disable-web-security` flag (development only):
```bash
chrome.exe --disable-web-security --user-data-dir=C:\temp\chrome-dev
```

---

### D7 — Booking returns 422/503 instead of 201

**Symptom:** Creating a booking fails even though equipment is available.

**Most likely cause:** Equipment Service is still processing from the circuit breaker demo — give it 10 seconds.

**Check:**
```bash
# Is equipment status AVAILABLE?
curl -s http://localhost:8080/api/equipment/1 \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
# If status is BOOKED, use equipment ID 2 instead

# Is circuit breaker CLOSED?
curl -s http://localhost:8081/actuator/circuitbreakers | python -m json.tool
```

---

### D8 — python -m json.tool gives error

**Symptom:** `No JSON object could be decoded` or blank output.

**Cause:** The API returned HTML (likely an error page) or the response is 204 No Content.

**Fix:** Remove `| python -m json.tool` and check raw output first:
```bash
curl -s -w "\n\nHTTP: %{http_code}" http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📋 SECTION E — Scene-Specific Tips

### Scene 7 (Booking + RabbitMQ) — The Most Important Scene

- **Do this BEFORE this scene:** Open RabbitMQ Management in a browser tab and navigate to the queue page. Have it ready to switch to immediately after hitting Enter on the booking command.
- **Timing is critical:** The Equipment Service consumes the event in under 500ms — you won't see the queue spike unless you switch to RabbitMQ tab immediately. If you miss it, say: *"The message was consumed almost instantly — let me show the result which proves it arrived."*
- **Always verify usageCount AFTER** — this is your visual proof that the async flow worked.
- If you want to **slow down the consumer** to make the queue visible, temporarily set `spring.rabbitmq.listener.simple.prefetch=1` and add a `Thread.sleep(3000)` in the consumer. But this is optional — the usageCount change is sufficient proof.

### Scene 10 (Circuit Breaker) — The Riskiest Scene

- **Run this scene LAST if you're short on time** — it disrupts the system and requires recovery
- **Always have `docker-compose start equipment-service` typed and ready** before you stop it
- **Don't run more than 15 failed requests** — you might need to wait for the open-state timer
- The wait duration in dev config is `10s` — count to 12 out loud on camera to fill the silence
- If the circuit doesn't close automatically: `docker-compose restart booking-service`

### Scene 11 (Zipkin) — Browser Tab Readiness

- **Always click "Run Query" again** just before starting this scene — traces are only kept in memory by default and the UI might show an old trace
- If traces are missing, make one fresh request just before switching to Zipkin tab
- When clicking into a trace, zoom into the Equipment Service span to show the `http.status_code` and `http.url` tags — this proves the Feign call is traced end-to-end

### Scene 16 (Deliverables) — Closing

- Scroll the checklist **slowly** — do not rush this. Each checkmark is a mark.
- Name the evidence file for each item as you pass it
- When you reach the 100/100 card, pause for 3 full seconds of silence before the closing line
- End with eye contact (if using webcam) on the closing sentence

---

## 🔄 SECTION F — If You Need to Re-Record a Scene

You don't need to re-do the entire 45-minute video. Record scenes separately and cut them together.

### Reset procedure between re-takes

```bash
# Full clean slate
docker-compose down -v          # removes all volumes (deletes DB data)
docker-compose up -d            # fresh start
# Wait 60 seconds for health checks to pass
docker-compose ps               # verify all 9 UP
```

### Scene-specific resets

```bash
# Reset only booking/equipment data (without full restart)
docker-compose exec booking-db  psql -U booking_user  -d booking_db  -c "TRUNCATE bookings CASCADE;"
docker-compose exec equipment-db psql -U equipment_user -d equipment_db -c "TRUNCATE equipment CASCADE;"

# Reset circuit breaker without restart
docker-compose restart booking-service

# Reset RabbitMQ queues
docker-compose exec rabbitmq rabbitmqctl purge_queue equipment.booking.queue
docker-compose exec rabbitmq rabbitmqctl purge_queue booking.dlq
```

---

## 🎓 SECTION G — Assessor Perspective — What They're Watching For

These are the moments assessors rewind and rewatch. Make each one crystal clear:

| Moment | What proves the mark | How to make it unmissable |
|---|---|---|
| Config Server returning config | The `${JWT_SECRET}` reference visible | Zoom in on that line in VS Code |
| All 4 services in Eureka | Service names clearly readable | Zoom browser to 125% |
| JWT 401 response | Status code `401` on screen | Read it out: *"four-oh-one"* |
| JWT 403 response | Status code `403` on screen | Read it out: *"four-oh-three"* |
| POST returning 201 | `"status": 201` in terminal | Highlight with mouse, say *"two-oh-one Created"* |
| DELETE returning 204 | `Expected 204: 204` in terminal | Say *"two-oh-four No Content — no body"* |
| Feign call visible in code | `@FeignClient` annotation | Zoom into annotation in VS Code |
| usageCount incrementing | Before=0, After=1 | Run both commands back-to-back, side by side if possible |
| Circuit breaker OPEN | `"state": "OPEN"` in JSON | Read it out: *"state is now OPEN"* |
| Fallback returning 503 | `"status": 503` in response | Read it out: *"five-oh-three"* |
| DLQ message count > 0 | Queue depth 1+ in RabbitMQ UI | Zoom into the queue depth number |
| Zipkin span waterfall | 4 spans visible, linked | Point to each span name |
| Correlation ID echoed | Header in both request and response | Split-screen if possible |
| ADR files exist | 3 files in docs/adr/ | Show file explorer tree |
| 30 git commits | `git log --oneline` showing 30 lines | Scroll slowly |

---

## 🧠 SECTION H — Last-Minute Mental Checklist

Run through this mentally **60 seconds before** you press Record:

```
[ ] All 9 docker containers are UP and HEALTHY
[ ] I have a fresh $TOKEN in my terminal (test it: curl ...equipment → 200)
[ ] Chrome tabs 1-5 are open on the correct pages
[ ] VS Code has all 10 files pre-opened in tabs
[ ] Terminal tabs are labelled DEMO and LOGS
[ ] Font sizes are 15pt+ in terminal and VS Code
[ ] Notifications are OFF / Do Not Disturb is ON
[ ] RabbitMQ queues are empty (purged)
[ ] Booking and equipment databases are clean (or have expected seed data)
[ ] I have this script open on my phone/tablet beside me
[ ] I know which scene I'm starting with and what the first line is
[ ] I have a glass of water nearby
```

---

## 💡 SECTION I — Pro Tips From Experienced Presenters

1. **Record in 10-minute chunks, not one 45-minute take.** Edit them together. One bad scene doesn't ruin everything.

2. **Name your recording files:** `scene-01-architecture.mp4`, `scene-05-jwt.mp4` etc. Easy to re-record individual scenes.

3. **Record a 30-second intro separately.** It's easier to record the introduction after you know the content went well.

4. **Don't apologise for silence.** When waiting for a command, staying silent and watching the terminal looks confident. Nervous filler words sound worse than silence.

5. **The assessor can pause the video.** You don't need to slow down so they can read every JSON field. Point to the important part, say what it means, move on.

6. **Show the working system first, explain the code second.** Assessors care that the system works. Code evidence is secondary proof. Scene 7 (working booking) is more valuable than Scene 8 (showing the consumer code).

7. **If a Docker container crashes during recording:** Don't panic. Say *"Let me restart that service — this is exactly the kind of operational scenario a microservices architecture is designed to handle."* Then restart it on camera. This actually demonstrates operational knowledge.

8. **Use `| python -m json.tool` on every response.** Pretty-printed JSON is dramatically more readable in a recording than a single-line blob.

9. **The RabbitMQ message queue spike is fast.** If you miss it, show the usageCount change instead — it's equally valid proof that the async event was processed.

10. **End 5 minutes early rather than 5 minutes late.** A tight, well-paced 40-minute screencast scores better than a rushed, rambling 50-minute one.

---

*These instructions were written to ensure a confident, professional, mark-maximising screencast submission.*  
*Good luck — the system is solid. Trust the preparation.*

---

*University Lab Equipment Booking & Maintenance Platform · Microservices Architecture Assignment*  
*Spring Boot 3.3 · Spring Cloud 2023 · RabbitMQ · PostgreSQL · OpenTelemetry · Docker*
