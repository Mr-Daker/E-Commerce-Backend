package com.shoppingapplication.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    TopicExchange orderEventsExchange(@Value("${order.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue orderEventsQueue(@Value("${order.events.queue}") String queueName,
                           @Value("${order.events.dead-letter-exchange}") String deadLetterExchange) {
        return QueueBuilder.durable(queueName).deadLetterExchange(deadLetterExchange).build();
    }

    @Bean
    Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(orderEventsExchange).with("order.#");
    }

    @Bean
    TopicExchange orderEventsDeadLetterExchange(@Value("${order.events.dead-letter-exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    Queue orderEventsDeadLetterQueue(@Value("${order.events.dead-letter-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    Binding orderEventsDeadLetterBinding(Queue orderEventsDeadLetterQueue,
                                         TopicExchange orderEventsDeadLetterExchange) {
        return BindingBuilder.bind(orderEventsDeadLetterQueue)
                .to(orderEventsDeadLetterExchange).with("order.#");
    }
}
