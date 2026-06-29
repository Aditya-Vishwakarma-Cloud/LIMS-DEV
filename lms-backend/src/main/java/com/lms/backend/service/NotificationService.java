package com.lms.backend.service;

import com.lms.backend.dto.NotificationDto;
import com.lms.backend.entity.NotificationPriority;
import com.lms.backend.entity.User;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDto> getUserNotifications(UUID userId);
    List<NotificationDto> getUnreadUserNotifications(UUID userId);
    NotificationDto markAsRead(UUID notificationId);
    void markAllAsRead(UUID userId);
    void createAndSendNotification(User user, String eventType, String title, String message, NotificationPriority priority);
}
