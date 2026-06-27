# ADR-002: Event-Driven Communication via RabbitMQ

**Status:** Accepted  
**Date:** 2024-06-17  
**Deciders:** Platform Architect, Backend Lead, Infrastructure Lead  
**Supersedes:** None  

---

## Context

When a student books equipment, two domain objects must be updated:
1. `Booking` (in `booking_db`) — the reservation record
2. `Equipment` (in `equipment_db`) — usage count, status (AVAILABLE → BOOKED), maintenance flag

**The communication between Booking Service and Equipment Service must be designed.** Forces:
- Both services own separate databases (Database-per-Service pattern)
- The booking confirmation must be fast — students should not wait for equipment updates
- Equipment Service may be temporarily unavailable (maintenance, crash)
- Duplicate event delivery (RabbitMQ AT-LEAST-ONCE guarantee) must not corrupt data
- Future consumers (e.g., Notification Service) may also need booking events

---

## Decision

**Use asynchronous messaging via RabbitMQ for post-booking equipment state updates. Use synchronous OpenFeign for pre-booking availability checks.**

```
POST /api/bookings
    ↓
BookingService.createBooking()
    ↓ [SYNC — Feign] checkAvailability() → Equipment Service (must succeed before booking)
    ↓
[DB] Save booking → booking_db
    ↓
[ASYNC — RabbitMQ] Publish BookingCreatedEventV1 → booking.events (Topic Exchange)
                                                   routing key: "booking.created"
                                                         ↓
                                               [equipment.booking.queue]
                                                         ↓
                                              Equipment Service consumer
                                                         ↓
                                               usageCount++, status=BOOKED
```

**Two-phase design rationale:**
- Availability check MUST be synchronous — booking must fail if equipment is unavailable
- Usage update can be asynchronous — eventual consistency is acceptable here

---

## Alternatives Considered

### Alternative A: Purely Synchronous REST (Feign for all communication)
- **Pros:** Simple; immediate consistency; easy to reason about
- **Cons:** Booking Service tightly coupled to Equipment Service availability; if Equipment Service is down, ALL bookings fail; cascading failures
- **Rejected:** Violates service autonomy; unacceptable availability impact

### Alternative B: Two-Phase Commit / Saga Pattern
- **Pros:** Stronger consistency guarantees; atomic across services
- **Cons:** Complex to implement; requires a Saga orchestrator or choreography state machine; high operational overhead
- **Rejected:** Over-engineering for this assessment; AT-LEAST-ONCE + idempotency is sufficient

### Alternative C: Apache Kafka
- **Pros:** Durable log; replay capability; partitioning for scale
- **Cons:** Requires ZooKeeper/KRaft setup; higher operational complexity; overkill for current scale
- **Rejected:** RabbitMQ is simpler for this use case and widely used in Spring Boot ecosystem

---

## Consequences

### Positive
- **Loose coupling** — Booking Service does not need Equipment Service to be up at publish time
- **Resilience** — messages queued in RabbitMQ survive Equipment Service restarts
- **Extensibility** — new consumers (Notification, Analytics) subscribe to `booking.events` exchange without changing Booking Service
- **Idempotency** — `eventId` (UUID) prevents double-processing on redelivery
- **DLQ safety** — failed messages go to `booking.dlq` for inspection, not lost

### Negative
- **Eventual consistency** — brief window where booking is confirmed but equipment status not yet updated
- **Operational complexity** — RabbitMQ requires management, monitoring, and DLQ inspection
- **Message ordering** — not guaranteed in topic exchanges (acceptable for this use case)

---

## Implementation Evidence

### Java Classes

| Class | Location | Role |
|-------|----------|------|
| `BookingCreatedEvent` | `common-library/.../event/BookingCreatedEvent.java` | Versioned event contract with `eventId`, `eventVersion`, `traceId` |
| `BookingCreatedEventV1` | `common-library/.../event/BookingCreatedEventV1.java` | Explicit V1 type — used as RabbitMQ `__TypeId__` header |
| `BookingEventPublisher` | `booking-service/.../messaging/BookingEventPublisher.java` | Publishes event after successful booking save, injects `traceId` |
| `RabbitMQConfig` (booking) | `booking-service/.../config/RabbitMQConfig.java` | Declares exchange, queue, DLQ, DLX, bindings, JSON converter |
| `BookingEventConsumer` | `equipment-service/.../messaging/BookingEventConsumer.java` | Idempotent consumer: eventId dedup, version check, manual ACK/NACK |

### Config Files

| File | Key Setting |
|------|------------|
| `config-repo/booking-service-dev.yml` | `spring.rabbitmq.publisher-confirm-type: correlated` |
| `config-repo/booking-service-dev.yml` | `spring.rabbitmq.listener.simple.acknowledge-mode: manual` |
| `config-repo/equipment-service-dev.yml` | `equipment.booking.queue.name: equipment.booking.queue` |
| `docker-compose.yml` | RabbitMQ container with management UI on port 15672 |

### RabbitMQ Topology

```
[booking.events] ← TopicExchange (durable)
        |
        ├── routing key: "booking.created"
        |         ↓
        |   [equipment.booking.queue] (durable)
        |         ├── x-dead-letter-exchange: booking.dlx
        |         ├── x-dead-letter-routing-key: booking.dead
        |         └── x-message-ttl: 86400000 (24h)
        |
        └── (future) routing key: "booking.notification"
                    ↓
              [notification.queue]

[booking.dlx] ← DirectExchange
        |
        └── routing key: "booking.dead"
                    ↓
              [booking.dlq] (durable — inspection queue)
```

### Event Versioning Strategy

```java
// common-library — explicit version constant
public static final String CURRENT_VERSION = "1.0";

// Publisher sets version on every event
BookingCreatedEvent event = BookingCreatedEvent.builder()
    .eventId(UUID.randomUUID().toString())  // idempotency key
    .eventVersion("1.0")                    // schema version
    .traceId(currentTraceId)               // distributed trace
    .bookingId(savedBooking.getId())
    .build();

// Consumer checks version before processing
if (!BookingCreatedEvent.CURRENT_VERSION.equals(event.getEventVersion())) {
    log.warn("Version mismatch — expected 1.0, got {}", event.getEventVersion());
    // NACK → DLQ for incompatible versions
}
```

### Idempotency Implementation

```java
// LRU cache — last 1000 processed eventIds
private final Map<String, Boolean> processedEventIds = Collections.synchronizedMap(
    new LinkedHashMap<>(1000, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> e) {
            return size() > 1000;
        }
    }
);

// Duplicate check before processing
if (processedEventIds.containsKey(event.getEventId())) {
    log.warn("DUPLICATE — skipping eventId={}", event.getEventId());
    channel.basicAck(deliveryTag, false); // ACK to clear from queue
    return;
}
```

### State Change Evidence

Before booking created:
```json
{ "id": 1, "name": "Oscilloscope", "usageCount": 5, "status": "AVAILABLE" }
```

After `BookingCreatedEventV1` consumed:
```json
{ "id": 1, "name": "Oscilloscope", "usageCount": 6, "status": "BOOKED" }
```

### Timestamp References
- `RabbitMQConfig` created: Sprint 2, commit `feat(booking): configure RabbitMQ topic exchange, DLQ`
- `BookingEventPublisher` created: Sprint 2, commit `feat(booking): add BookingEventPublisher with trace ID injection`
- `BookingEventConsumer` created: Sprint 2, commit `feat(equipment): add idempotent BookingEventConsumer`
- `BookingCreatedEventV1` created: Sprint 3, commit `feat(common): add BookingCreatedEventV1 for explicit versioning`
