package com.labplatform.equipment.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
@Configuration
public class RabbitMQConsumerConfig {
    public static final String EQUIPMENT_BOOKING_QUEUE = "equipment.booking.queue";
    public static final String EQUIPMENT_EVENTS_EXCHANGE = "equipment.events";
    public static final String EQUIPMENT_BOOKING_REJECTED_ROUTING_KEY = "equipment.booking.rejected";

    @Bean
    public TopicExchange equipmentEventsExchange() {
        return new TopicExchange(EQUIPMENT_EVENTS_EXCHANGE);
    }

    @Bean
    public @NonNull MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public RabbitTemplate rabbitTemplate(@NonNull ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            @NonNull ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
