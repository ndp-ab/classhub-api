package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.NotificationResponse;
import com.classhub.classhubapi.dto.UnreadCountResponse;
import com.classhub.classhubapi.entity.*;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.exception.ForbiddenException;
import com.classhub.classhubapi.repository.ClassroomRepository;
import com.classhub.classhubapi.repository.NotificationRecipientRepository;
import com.classhub.classhubapi.repository.NotificationRepository;
import com.classhub.classhubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRecipientRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return UnreadCountResponse.builder()
                .count(notificationRecipientRepository.countByUserIdAndReadFalse(userId))
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long recipientId, Long userId) {
        NotificationRecipient recipient = notificationRecipientRepository
                .findByIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen thao tac thong bao nay"));

        if (!recipient.isRead()) {
            recipient.setRead(true);
            recipient.setReadAt(LocalDateTime.now());
            notificationRecipientRepository.save(recipient);
        }

        return toResponse(recipient);
    }

    @Transactional
    public UnreadCountResponse markAllAsRead(Long userId) {
        List<NotificationRecipient> unreadRecipients =
                notificationRecipientRepository.findByUserIdAndReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();

        unreadRecipients.forEach(recipient -> {
            recipient.setRead(true);
            recipient.setReadAt(now);
        });
        notificationRecipientRepository.saveAll(unreadRecipients);

        return UnreadCountResponse.builder()
                .count(0)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(
            Long classroomId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            Long targetId,
            Long createdByUserId,
            Collection<Long> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }

        List<Long> recipientIds = recipientUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (recipientIds.isEmpty()) {
            return;
        }

        Classroom classroom = classroomId != null
                ? classroomRepository.findById(classroomId)
                        .orElseThrow(() -> new BadRequestException("Lop hoc khong ton tai"))
                : null;
        User createdBy = createdByUserId != null
                ? userRepository.findById(createdByUserId)
                        .orElseThrow(() -> new BadRequestException("User khong ton tai"))
                : null;
        List<User> recipients = userRepository.findAllById(recipientIds);
        if (recipients.size() != recipientIds.size()) {
            throw new BadRequestException("Recipient khong ton tai");
        }

        Notification notification = Notification.builder()
                .classroom(classroom)
                .type(type)
                .title(title)
                .message(message)
                .targetType(targetType)
                .targetId(targetId)
                .createdBy(createdBy)
                .build();
        notificationRepository.save(notification);

        List<NotificationRecipient> notificationRecipients = recipients.stream()
                .map(user -> NotificationRecipient.builder()
                        .notification(notification)
                        .user(user)
                        .build())
                .collect(Collectors.toList());
        notificationRecipientRepository.saveAll(notificationRecipients);
    }

    private NotificationResponse toResponse(NotificationRecipient recipient) {
        Notification notification = recipient.getNotification();
        Classroom classroom = notification.getClassroom();
        User createdBy = notification.getCreatedBy();

        return NotificationResponse.builder()
                .recipientId(recipient.getId())
                .notificationId(notification.getId())
                .classroomId(classroom != null ? classroom.getId() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .isRead(recipient.isRead())
                .readAt(recipient.getReadAt())
                .createdAt(notification.getCreatedAt())
                .createdByName(createdBy != null ? createdBy.getFullName() : null)
                .build();
    }
}
