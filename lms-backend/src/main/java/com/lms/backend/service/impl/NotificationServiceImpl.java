package com.lms.backend.service.impl;

import com.lms.backend.dto.NotificationDto;
import com.lms.backend.entity.AccountStatus;
import com.lms.backend.entity.Notification;
import com.lms.backend.entity.NotificationPriority;
import com.lms.backend.entity.User;
import com.lms.backend.event.LimsNotificationEvent;
import com.lms.backend.repository.NotificationRepository;
import com.lms.backend.repository.UserRepository;
import com.lms.backend.service.EmailService;
import com.lms.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(UUID notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notif.setRead(true);
        Notification saved = notificationRepository.save(notif);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void createAndSendNotification(User user, String eventType, String title, String message, NotificationPriority priority) {
        log.info("Creating notification for user: {} ({}) - Title: {}", user.getEmail(), user.getName(), title);
        
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .priority(priority)
                .eventType(eventType)
                .read(false)
                .emailSent(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Send email asynchronously/directly
        try {
            // Reusable HTML Template
            String emailContent = String.format(
                    "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "  <style>" +
                    "    body { font-family: sans-serif; color: #1e293b; line-height: 1.5; }" +
                    "    .container { max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }" +
                    "    .header { background: #1d4ed8; color: #ffffff; padding: 24px; text-align: center; }" +
                    "    .body { padding: 32px; }" +
                    "    .footer { background: #f8fafc; color: #64748b; padding: 16px; text-align: center; font-size: 12px; }" +
                    "    .priority-critical { color: #dc2626; font-weight: bold; }" +
                    "    .priority-high { color: #ea580c; font-weight: bold; }" +
                    "    .priority-medium { color: #2563eb; font-weight: bold; }" +
                    "    .priority-low { color: #475569; font-weight: bold; }" +
                    "  </style>" +
                    "</head>" +
                    "<body>" +
                    "  <div class='container'>" +
                    "    <div class='header'><h2>WeMurz LIMS Alert</h2></div>" +
                    "    <div class='body'>" +
                    "      <p>Hello %s,</p>" +
                    "      <p>A new event has occurred in the LIMS workspace:</p>" +
                    "      <blockquote style='background: #f1f5f9; padding: 16px; border-left: 4px solid #1d4ed8; margin: 16px 0;'>" +
                    "        <strong>%s</strong><br/>" +
                    "        <span class='priority-%s'>Priority: %s</span><br/>" +
                    "        <p style='margin-top: 8px;'>%s</p>" +
                    "      </blockquote>" +
                    "      <p>Please log in to your dashboard to review details.</p>" +
                    "    </div>" +
                    "    <div class='footer'>Regards, WeMurz Materials Laboratory</div>" +
                    "  </div>" +
                    "</body>" +
                    "</html>",
                    user.getName(),
                    title,
                    priority.name().toLowerCase(),
                    priority.name(),
                    message
            );

            emailService.sendEmail(user.getEmail(), "LIMS Notice: " + title, emailContent);
            saved.setEmailSent(true);
            notificationRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to send notification email: ", e);
        }
    }

    /**
     * Listener for decoupled event-driven architecture
     */
    @EventListener
    @Transactional
    public void handleLimsNotificationEvent(LimsNotificationEvent event) {
        log.info("Received LimsNotificationEvent for type: {}", event.getEventType());

        // 1. Direct Recipient if Email is provided
        if (event.getRecipientEmail() != null && !event.getRecipientEmail().isEmpty()) {
            userRepository.findByEmail(event.getRecipientEmail()).ifPresent(user -> 
                createAndSendNotification(user, event.getEventType(), event.getTitle(), event.getMessage(), event.getPriority())
            );
        }

        // 2. Broadcast to specific Roles if recipientRole is provided
        if (event.getRecipientRole() != null && !event.getRecipientRole().isEmpty()) {
            String[] roles = event.getRecipientRole().split(",");
            for (String roleName : roles) {
                List<User> targets = userRepository.findByRoleAndStatusAndNotDeleted(roleName.trim(), AccountStatus.ACTIVE);
                for (User target : targets) {
                    createAndSendNotification(target, event.getEventType(), event.getTitle(), event.getMessage(), event.getPriority());
                }
            }
        }
    }

    private NotificationDto mapToDto(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .priority(entity.getPriority())
                .eventType(entity.getEventType())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
