package com.shoppingapplication.notificationservice.repository;
import com.shoppingapplication.notificationservice.model.NotificationPreference; import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,String> {}
