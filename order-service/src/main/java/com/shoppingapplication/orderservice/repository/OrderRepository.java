package com.shoppingapplication.orderservice.repository;

import com.shoppingapplication.orderservice.model.Order;
import com.shoppingapplication.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByStatus(OrderStatus status);
    @Query("select o from Order o where o.status = :status and " +
            "((o.recoveryAfter is not null and o.recoveryAfter < :now) or " +
            "(o.recoveryAfter is null and o.createdAt < :legacyCutoff))")
    List<Order> findRecoverablePending(@Param("status") OrderStatus status,
                                       @Param("now") Instant now,
                                       @Param("legacyCutoff") Instant legacyCutoff);
}
