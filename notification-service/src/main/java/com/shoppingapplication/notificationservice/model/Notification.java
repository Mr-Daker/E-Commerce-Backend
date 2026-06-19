package com.shoppingapplication.notificationservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "t_notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String eventId;
    @Column(nullable = false)
    private String orderNumber;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(nullable = false)
    private Instant receivedAt;
    private Instant readAt;
    @Column(nullable = false)
    private Boolean visible = true;
}
