package com.shoppingapplication.notificationservice.repository;

import com.shoppingapplication.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);
    Page<Notification> findByUserIdAndVisibleTrue(String userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(Long id, String userId);
    long countByUserIdAndVisibleTrueAndReadAtIsNull(String userId);
    @Modifying
    @Query("update Notification n set n.readAt=:now where n.userId=:userId and n.readAt is null")
    int markAllRead(@Param("userId") String userId, @Param("now") Instant now);
}
