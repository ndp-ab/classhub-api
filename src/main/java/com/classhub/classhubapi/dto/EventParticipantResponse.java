package com.classhub.classhubapi.dto;

import com.classhub.classhubapi.entity.CheckinSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventParticipantResponse {

    private Long id;
    private Long eventId;          // Flutter cần để match my-events với danh sách sự kiện
    private Long userId;
    private String fullName;
    private String eventTitle;

    // Đã được check-in chưa
    private boolean checkedIn;
    private LocalDateTime checkedInAt;
    private String checkedByName;  // Audit (B4): ai đã check-in

    // Thời điểm đăng ký tham gia
    private LocalDateTime registeredAt;

    private Long checkinSubmissionId;
    private CheckinSubmissionStatus checkinSubmissionStatus;
    private String checkinImageUrl;
    private LocalDateTime checkinSubmittedAt;
}
