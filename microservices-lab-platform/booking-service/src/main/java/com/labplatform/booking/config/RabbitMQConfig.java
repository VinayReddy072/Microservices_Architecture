package com.labplatform.booking.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
@Configuration
public class RabbitMQConfig {
    public static final String BOOKING_EVENTS_EXCHANGE = "booking.events";
    public static final String BOOKING_DLX_EXCHANGE = "booking.dlx";
    public static final String EQUIPMENT_BOOKING_QUEUE = "equipment.booking.queue";
    public static final String BOOKING_DLQ = "booking.dlq";
    public static final String BOOKING_CREATED_ROUTING_KEY = "booking.created";
    
    public static final String EQUIPMENT_EVENTS_EXCHANGE = "equipment.events";
    public static final String EQUIPMENT_BOOKING_REJECTED_QUEUE = "equipment.booking.rejected.queue";
    public static final String EQUIPMENT_BOOKING_REJECTED_ROUTING_KEY = "equipment.booking.rejected";
    @Bean
    public TopicExchange bookingEventsExchange() {
        return ExchangeBuilder.topicExchange(BOOKING_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    @Bean
    public DirectExchange bookingDeadLetterExchange() {
        return ExchangeBuilder.directExchange(BOOKING_DLX_EXCHANGE)
                .durable(true)
                .build();
    }
    @Bean
    public Queue equipmentBookingQueue() {
        return QueueBuilder.durable(EQUIPMENT_BOOKING_QUEUE)
                .withArgument("x-dead-letter-exchange", BOOKING_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "booking.dead")
                .withArgument("x-message-ttl", 86400000)  
                .build();
    }
    @Bean
    public Queue bookingDeadLetterQueue() {
        return QueueBuilder.durable(BOOKING_DLQ).build();
    }
    @Bean
    public Binding equipmentQueueBinding() {
        return BindingBuilder.bind(equipmentBookingQueue())
                .to(bookingEventsExchange())
                .with(BOOKING_CREATED_ROUTING_KEY);
    }
    
    @Bean
    public TopicExchange equipmentEventsExchange() {
        return ExchangeBuilder.topicExchange(EQUIPMENT_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue equipmentBookingRejectedQueue() {
        return QueueBuilder.durable(EQUIPMENT_BOOKING_REJECTED_QUEUE).build();
    }

    @Bean
    public Binding equipmentRejectedBinding() {
        return BindingBuilder.bind(equipmentBookingRejectedQueue())
                .to(equipmentEventsExchange())
                .with(EQUIPMENT_BOOKING_REJECTED_ROUTING_KEY);
    }
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(bookingDeadLetterQueue())
                .to(bookingDeadLetterExchange())
                .with("booking.dead");
    }
    @Bean
    public @NonNull MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public RabbitTemplate rabbitTemplate(@NonNull ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .error("Message publish NACK: correlationData={} cause={}", correlationData, cause);
            }
        });
        return template;
    }
}
