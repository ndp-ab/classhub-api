package com.classhub.classhubapi.dto;

import com.classhub.classhubapi.entity.NotificationTargetType;
import com.classhub.classhubapi.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long recipientId;
    private Long notificationId;
    private Long classroomId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationTargetType targetType;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String createdByName;
}
