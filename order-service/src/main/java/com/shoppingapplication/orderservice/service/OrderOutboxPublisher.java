package com.shoppingapplication.orderservice.service;

import com.shoppingapplication.orderservice.model.OrderOutboxEvent;
import com.shoppingapplication.orderservice.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisher {
    private final OrderOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${order.events.exchange}")
    private String exchange;
    @Value("${order.events.routing-key}")
    private String routingKey;

    @Scheduled(fixedDelayString = "${order.outbox.publish-delay-ms:1000}")
    public void publishPendingEvents() {
        outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc().forEach(this::publish);
    }

    private void publish(OrderOutboxEvent event) {
        try {
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setMessageId(event.getId())
                    .setHeader("eventType", event.getEventType())
                    .build();
            CorrelationData correlationData = new CorrelationData(event.getId());
            rabbitTemplate.send(exchange, routingKey, message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
            }
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            outboxRepository.save(event);
        } catch (Exception exception) {
            event.setAttempts(event.getAttempts() + 1);
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            event.setLastError(error.substring(0, Math.min(error.length(), 500)));
            outboxRepository.save(event);
            log.warn("Could not publish order event {}; it remains in the outbox", event.getId());
        }
    }
}
