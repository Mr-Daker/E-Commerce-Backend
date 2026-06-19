package com.shoppingapplication.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String eventId;
    private String orderNumber;
    private String eventType;
    private String message;
    private Instant receivedAt;
    private Instant readAt;
}
