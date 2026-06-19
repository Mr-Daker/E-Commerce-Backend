package com.shoppingapplication.orderservice.repository;

import com.shoppingapplication.orderservice.model.OrderOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, String> {
    List<OrderOutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
