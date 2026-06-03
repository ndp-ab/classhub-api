package com.classhub.classhubapi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime eventTime;
    private String createdByName;

    // Tổng số người đã đăng ký tham gia
    private int volunteerCount;

    // Số người đã được check-in
    private int checkedInCount;

    private LocalDateTime createdAt;
}
