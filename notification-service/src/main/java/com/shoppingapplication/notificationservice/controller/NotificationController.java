package com.shoppingapplication.notificationservice.controller;

import com.shoppingapplication.notificationservice.dto.ApiResponse;
import com.shoppingapplication.notificationservice.dto.PagedNotificationResponse;
import com.shoppingapplication.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import javax.validation.Valid;
import com.shoppingapplication.notificationservice.dto.NotificationPreferenceRequest;
import com.shoppingapplication.notificationservice.dto.NotificationResponse;
import com.shoppingapplication.notificationservice.model.NotificationPreference;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PagedNotificationResponse> getNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Notifications fetched", notificationService.getNotifications(userId,page, size));
    }
    @GetMapping("/unread-count") public ApiResponse<Long> unread(@RequestHeader("X-User-Id") String user){return ApiResponse.success("Unread count",notificationService.unreadCount(user));}
    @PatchMapping("/{id}/read") public ApiResponse<NotificationResponse> read(@RequestHeader("X-User-Id") String user,@PathVariable Long id){return ApiResponse.success("Notification read",notificationService.markRead(user,id));}
    @PatchMapping("/read-all") public ApiResponse<Integer> readAll(@RequestHeader("X-User-Id") String user){return ApiResponse.success("Notifications read",notificationService.markAllRead(user));}
    @GetMapping("/preferences") public ApiResponse<NotificationPreference> preferences(@RequestHeader("X-User-Id") String user){return ApiResponse.success("Preferences fetched",notificationService.preferences(user));}
    @PutMapping("/preferences") public ApiResponse<NotificationPreference> preferences(@RequestHeader("X-User-Id") String user,@Valid @RequestBody NotificationPreferenceRequest r){return ApiResponse.success("Preferences updated",notificationService.updatePreferences(user,r));}
}
