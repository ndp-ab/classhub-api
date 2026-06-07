package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.EventCheckinSubmissionResponse;
import com.classhub.classhubapi.dto.RejectCheckinSubmissionRequest;
import com.classhub.classhubapi.service.EventCheckinSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventCheckinSubmissionController {

    private final EventCheckinSubmissionService submissionService;

    @PostMapping("/{eventId}/checkin-submissions")
    public ResponseEntity<EventCheckinSubmissionResponse> submit(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                submissionService.submit(eventId, SecurityUtil.currentUserId(), file));
    }

    @GetMapping("/{eventId}/checkin-submissions")
    public ResponseEntity<List<EventCheckinSubmissionResponse>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(
                submissionService.getByEvent(eventId, SecurityUtil.currentUserId()));
    }

    @PutMapping("/checkin-submissions/{submissionId}/approve")
    public ResponseEntity<EventCheckinSubmissionResponse> approve(@PathVariable Long submissionId) {
        return ResponseEntity.ok(
                submissionService.approve(submissionId, SecurityUtil.currentUserId()));
    }

    @PutMapping("/checkin-submissions/{submissionId}/reject")
    public ResponseEntity<EventCheckinSubmissionResponse> reject(
            @PathVariable Long submissionId,
            @Valid @RequestBody RejectCheckinSubmissionRequest request) {
        return ResponseEntity.ok(
                submissionService.reject(submissionId, SecurityUtil.currentUserId(), request.getReason()));
    }
}
