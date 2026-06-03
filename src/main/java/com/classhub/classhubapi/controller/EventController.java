package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.CreateEventRequest;
import com.classhub.classhubapi.dto.EventParticipantResponse;
import com.classhub.classhubapi.dto.EventResponse;
import com.classhub.classhubapi.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Admin tạo sự kiện
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request, SecurityUtil.currentUserId()));
    }

    // Danh sách sự kiện của 1 lớp
    @GetMapping("/{classroomId}")
    public ResponseEntity<List<EventResponse>> getByClassroom(@PathVariable Long classroomId) {
        return ResponseEntity.ok(
                eventService.getEventsByClassroom(classroomId, SecurityUtil.currentUserId()));
    }

    // Sinh viên đăng ký tham gia
    @PostMapping("/{eventId}/volunteer")
    public ResponseEntity<EventParticipantResponse> volunteer(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.volunteer(eventId, SecurityUtil.currentUserId()));
    }

    // Sinh viên huỷ đăng ký
    @DeleteMapping("/{eventId}/volunteer")
    public ResponseEntity<Void> cancelVolunteer(@PathVariable Long eventId) {
        eventService.cancelVolunteer(eventId, SecurityUtil.currentUserId());
        return ResponseEntity.noContent().build();
    }

    // Admin xem danh sách người đăng ký
    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<EventParticipantResponse>> getParticipants(@PathVariable Long eventId) {
        return ResponseEntity.ok(
                eventService.getParticipants(eventId, SecurityUtil.currentUserId()));
    }

    // Admin check-in (B4: lưu ai check-in)
    @PutMapping("/{eventId}/checkin/{userId}")
    public ResponseEntity<EventParticipantResponse> checkIn(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                eventService.checkIn(eventId, userId, SecurityUtil.currentUserId()));
    }

    // Sinh viên xem sự kiện mình đã đăng ký
    @GetMapping("/my/{classroomId}")
    public ResponseEntity<List<EventParticipantResponse>> getMyEvents(@PathVariable Long classroomId) {
        return ResponseEntity.ok(
                eventService.getMyEvents(SecurityUtil.currentUserId(), classroomId));
    }
}
