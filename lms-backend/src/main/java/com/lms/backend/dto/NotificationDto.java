package com.lms.backend.dto;

import com.lms.backend.entity.NotificationPriority;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationPriority priority;
    private String eventType;
    private boolean read;
    private LocalDateTime createdAt;
}
