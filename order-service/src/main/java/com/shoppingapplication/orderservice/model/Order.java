package com.shoppingapplication.orderservice.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="t_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @Column(nullable = false, unique = true)
    private String orderNumber;
    @Column(unique = true, updatable = false)
    private String idempotencyKey;
    @Column(updatable = false, length = 64)
    private String requestHash;
    @Column
    private String userId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private java.time.Instant createdAt;
    private java.time.Instant recoveryAfter;
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderLineItems> orderLineItemsList;
}
