package com.labplatform.booking.messaging;
import com.labplatform.booking.config.RabbitMQConfig;
import com.labplatform.common.event.BookingCreatedEvent;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    public void publishBookingCreated(BookingCreatedEvent event) {
        if (tracer != null && tracer.currentSpan() != null) {
            event.setTraceId(tracer.currentSpan().context().traceId());
        }
        log.info("Publishing BookingCreatedEvent: bookingId={} equipmentId={} userId={} traceId={}",
                event.getBookingId(), event.getEquipmentId(), event.getUserId(), event.getTraceId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EVENTS_EXCHANGE,
                    RabbitMQConfig.BOOKING_CREATED_ROUTING_KEY,
                    event
            );
            log.info("Successfully published BookingCreatedEvent for bookingId={}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to publish BookingCreatedEvent for bookingId={}: {}",
                    event.getBookingId(), e.getMessage(), e);
        }
    }
}
