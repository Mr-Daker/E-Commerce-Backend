package com.shoppingapplication.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingapplication.orderservice.model.Order;
import com.shoppingapplication.orderservice.model.OrderOutboxEvent;
import com.shoppingapplication.orderservice.repository.OrderOutboxRepository;
import com.shoppingapplication.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {
    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order saveWithEvent(Order order, String eventType) {
        Order savedOrder = orderRepository.save(order);
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("orderNumber", savedOrder.getOrderNumber());
        payload.put("userId", savedOrder.getUserId());
        payload.put("status", savedOrder.getStatus().name());
        payload.put("occurredAt", Instant.now());

        OrderOutboxEvent event = new OrderOutboxEvent();
        event.setId(eventId);
        event.setAggregateId(savedOrder.getOrderNumber());
        event.setEventType(eventType);
        event.setPayload(toJson(payload));
        event.setCreatedAt(Instant.now());
        event.setAttempts(0);
        outboxRepository.save(event);
        return savedOrder;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize order event", exception);
        }
    }
}
