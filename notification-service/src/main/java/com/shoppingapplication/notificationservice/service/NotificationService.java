package com.shoppingapplication.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingapplication.notificationservice.dto.NotificationResponse;
import com.shoppingapplication.notificationservice.dto.PagedNotificationResponse;
import com.shoppingapplication.notificationservice.model.Notification;
import com.shoppingapplication.notificationservice.repository.NotificationRepository;
import com.shoppingapplication.notificationservice.repository.NotificationPreferenceRepository;
import com.shoppingapplication.notificationservice.model.NotificationPreference;
import com.shoppingapplication.notificationservice.dto.NotificationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationPreferenceRepository preferenceRepository;

    @RabbitListener(queues = "${order.events.queue}")
    @Transactional
    public void consumeOrderEvent(String payload) {
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new AmqpRejectAndDontRequeueException("Invalid order event", exception);
        }
        String eventId = requiredText(event, "eventId");
        if (notificationRepository.existsByEventId(eventId)) {
            return;
        }

        String eventType = requiredText(event, "eventType");
        String orderNumber = requiredText(event, "orderNumber");
        String userId = event.hasNonNull("userId") && !event.path("userId").asText().isBlank() ? event.path("userId").asText() : "system";
        NotificationPreference preference = preferences(userId);
        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setEventType(eventType);
        notification.setOrderNumber(orderNumber);
        notification.setUserId(userId);
        notification.setMessage(messageFor(eventType, orderNumber));
        notification.setReceivedAt(Instant.now());
        notification.setVisible(isEnabled(preference, eventType));
        notificationRepository.save(notification);
        log.info("notification eventId={} orderNumber={} type={}", eventId, orderNumber, eventType);
    }

    @Transactional(readOnly = true)
    public PagedNotificationResponse getNotifications(String userId, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Page must be zero or greater and size must be at least one");
        }
        Page<Notification> result = notificationRepository.findByUserIdAndVisibleTrue(userId,
                PageRequest.of(page, Math.min(size, 100), Sort.by("receivedAt").descending()));
        return PagedNotificationResponse.builder()
                .content(result.map(this::toResponse).getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public NotificationResponse markRead(String userId, Long id) {
        Notification notification=notificationRepository.findByIdAndUserId(id,userId).orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if(notification.getReadAt()==null) notification.setReadAt(Instant.now()); return toResponse(notification);
    }
    @Transactional public int markAllRead(String userId){return notificationRepository.markAllRead(userId,Instant.now());}
    @Transactional(readOnly=true) public long unreadCount(String userId){return notificationRepository.countByUserIdAndVisibleTrueAndReadAtIsNull(userId);}
    @Transactional(readOnly=true) public NotificationPreference preferences(String userId){return preferenceRepository.findById(userId).orElseGet(()->{NotificationPreference p=new NotificationPreference();p.setUserId(userId);return p;});}
    @Transactional public NotificationPreference updatePreferences(String userId, NotificationPreferenceRequest r){NotificationPreference p=preferences(userId);p.setOrderConfirmed(r.getOrderConfirmed());p.setOrderCancelled(r.getOrderCancelled());p.setOrderFailed(r.getOrderFailed());return preferenceRepository.save(p);}
    private boolean isEnabled(NotificationPreference p,String type){return switch(type){case "ORDER_CONFIRMED"->p.getOrderConfirmed();case "ORDER_CANCELLED"->p.getOrderCancelled();case "ORDER_FAILED","ORDER_COMPENSATION_REQUIRED"->p.getOrderFailed();default->true;};}

    private String requiredText(JsonNode event, String field) {
        JsonNode value = event.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Missing event field: " + field);
        }
        return value.asText();
    }

    private String messageFor(String eventType, String orderNumber) {
        return switch (eventType) {
            case "ORDER_CONFIRMED" -> "Order " + orderNumber + " was confirmed.";
            case "ORDER_CANCELLED" -> "Order " + orderNumber + " was cancelled.";
            case "ORDER_FAILED" -> "Order " + orderNumber + " could not be completed.";
            case "ORDER_COMPENSATION_REQUIRED" -> "Order " + orderNumber + " requires recovery.";
            default -> "Order " + orderNumber + " changed state: " + eventType + ".";
        };
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .orderNumber(notification.getOrderNumber())
                .eventType(notification.getEventType())
                .message(notification.getMessage())
                .receivedAt(notification.getReceivedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
