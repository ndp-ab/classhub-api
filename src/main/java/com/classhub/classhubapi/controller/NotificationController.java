package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.NotificationResponse;
import com.classhub.classhubapi.dto.UnreadCountResponse;
import com.classhub.classhubapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(
                SecurityUtil.currentUserId(),
                PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount(SecurityUtil.currentUserId()));
    }

    @PutMapping("/{recipientId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.markAsRead(
                recipientId,
                SecurityUtil.currentUserId()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<UnreadCountResponse> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead(SecurityUtil.currentUserId()));
    }
}
