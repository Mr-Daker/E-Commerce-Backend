package com.shoppingapplication.orderservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "t_order_outbox")
@Getter
@Setter
@NoArgsConstructor
public class OrderOutboxEvent {
    @Id
    private String id;
    @Column(nullable = false)
    private String aggregateId;
    @Column(nullable = false)
    private String eventType;
    @Lob
    @Column(nullable = false)
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;
    @Column(nullable = false)
    private Integer attempts = 0;
    @Column(length = 500)
    private String lastError;
}
