package com.labplatform.equipment.messaging;
import com.labplatform.common.event.BookingCreatedEvent;
import com.labplatform.equipment.service.EquipmentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.labplatform.common.event.BookingRejectedEvent;
import com.labplatform.equipment.config.RabbitMQConsumerConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {
    private final EquipmentService equipmentService;
    private final RabbitTemplate rabbitTemplate;
    private static final int DEDUP_CACHE_SIZE = 1000;
    private final Map<String, Boolean> processedEventIds = Collections.synchronizedMap(
            new LinkedHashMap<>(DEDUP_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > DEDUP_CACHE_SIZE;
                }
            }
    );
    @RabbitListener(
        queues = "${equipment.booking.queue.name:equipment.booking.queue}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void handleBookingCreated(
            BookingCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (event.getEventId() != null && processedEventIds.containsKey(event.getEventId())) {
            log.warn("DUPLICATE event detected — skipping already-processed eventId={} bookingId={}",
                    event.getEventId(), event.getBookingId());
            safeAck(channel, deliveryTag);
            return;
        }
        String receivedVersion = event.getEventVersion();
        if (receivedVersion != null && !BookingCreatedEvent.CURRENT_VERSION.equals(receivedVersion)) {
            log.warn("Event version mismatch: expected={} received={} for bookingId={}. " +
                    "Attempting processing — review if data anomalies occur.",
                    BookingCreatedEvent.CURRENT_VERSION, receivedVersion, event.getBookingId());
        }
        if (event.getTraceId() != null) {
            MDC.put("traceId", event.getTraceId());
        }
        if (event.getEventId() != null) {
            MDC.put("eventId", event.getEventId());
        }
        log.info("Processing BookingCreatedEvent: eventId={} version={} bookingId={} equipmentId={} userId={}",
                event.getEventId(), event.getEventVersion(),
                event.getBookingId(), event.getEquipmentId(), event.getUserId());
        try {
            boolean success = equipmentService.processBookingCreatedEvent(event.getEquipmentId());
            if (!success) {
                log.warn("Publishing BookingRejectedEvent for bookingId={}", event.getBookingId());
                BookingRejectedEvent rejection = BookingRejectedEvent.builder()
                        .bookingId(event.getBookingId())
                        .equipmentId(event.getEquipmentId())
                        .reason("Equipment requires maintenance or is unavailable")
                        .build();
                rabbitTemplate.convertAndSend(
                        RabbitMQConsumerConfig.EQUIPMENT_EVENTS_EXCHANGE,
                        RabbitMQConsumerConfig.EQUIPMENT_BOOKING_REJECTED_ROUTING_KEY,
                        rejection
                );
            }
            if (event.getEventId() != null) {
                processedEventIds.put(event.getEventId(), Boolean.TRUE);
            }
            safeAck(channel, deliveryTag);
            log.info("BookingCreatedEvent ACK'd: eventId={} bookingId={} equipmentId={}",
                    event.getEventId(), event.getBookingId(), event.getEquipmentId());
        } catch (Exception e) {
            log.error("Failed to process BookingCreatedEvent: eventId={} bookingId={} equipmentId={} error={}",
                    event.getEventId(), event.getBookingId(), event.getEquipmentId(), e.getMessage(), e);
            safeNack(channel, deliveryTag);
            log.warn("BookingCreatedEvent NACK'd to DLQ: eventId={} bookingId={}",
                    event.getEventId(), event.getBookingId());
        } finally {
            MDC.remove("traceId");
            MDC.remove("eventId");
        }
    }
    private void safeAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("Failed to ACK message deliveryTag={}: {}", deliveryTag, e.getMessage());
        }
    }
    private void safeNack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("Failed to NACK message deliveryTag={}: {}", deliveryTag, e.getMessage());
        }
    }
}
