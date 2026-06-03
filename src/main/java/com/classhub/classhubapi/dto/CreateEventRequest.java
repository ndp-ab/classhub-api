package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    private String title;

    // Mô tả không bắt buộc
    private String description;

    private String location;

    @NotNull(message = "Thời gian sự kiện không được để trống")
    private LocalDateTime eventTime;

    @NotNull(message = "Lớp học không được để trống")
    private Long classroomId;
}
