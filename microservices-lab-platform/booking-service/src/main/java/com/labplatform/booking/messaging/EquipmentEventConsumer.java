package com.labplatform.booking.messaging;

import com.labplatform.booking.service.BookingService;
import com.labplatform.common.event.BookingRejectedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentEventConsumer {

    private final BookingService bookingService;

    @RabbitListener(queues = "${equipment.booking.rejected.queue.name:equipment.booking.rejected.queue}")
    public void handleBookingRejected(
            BookingRejectedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        log.warn("Processing BookingRejectedEvent: bookingId={} equipmentId={} reason={}",
                event.getBookingId(), event.getEquipmentId(), event.getReason());
        
        try {
            bookingService.cancelBooking(event.getBookingId());
            log.info("Successfully applied compensating transaction to cancel bookingId={}", event.getBookingId());
            safeAck(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Failed to cancel bookingId={}: {}", event.getBookingId(), e.getMessage(), e);
            safeNack(channel, deliveryTag);
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
