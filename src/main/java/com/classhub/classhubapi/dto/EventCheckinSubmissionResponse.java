package com.classhub.classhubapi.dto;

import com.classhub.classhubapi.entity.CheckinSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventCheckinSubmissionResponse {

    private Long id;
    private Long eventId;
    private Long userId;
    private String fullName;
    private String imageUrl;
    private CheckinSubmissionStatus status;
    private LocalDateTime submittedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String rejectedReason;
}
