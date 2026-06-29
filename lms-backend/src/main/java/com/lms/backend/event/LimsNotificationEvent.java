package com.lms.backend.event;

import com.lms.backend.entity.NotificationPriority;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LimsNotificationEvent extends ApplicationEvent {
    private final String eventType;
    private final String title;
    private final String message;
    private final NotificationPriority priority;
    private final String recipientRole; // Target role(s) to notify (comma-separated or single)
    private final String recipientEmail; // Direct recipient email if applicable

    public LimsNotificationEvent(Object source, String eventType, String title, String message, 
                                 NotificationPriority priority, String recipientRole, String recipientEmail) {
        super(source);
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.priority = priority;
        this.recipientRole = recipientRole;
        this.recipientEmail = recipientEmail;
    }
}
