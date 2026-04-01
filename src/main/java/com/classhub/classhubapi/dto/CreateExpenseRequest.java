package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateExpenseRequest {

    @NotBlank(message = "Tiêu đề chi phí không được để trống")
    private String title;

    @NotNull(message = "Số tiền chi phí không được để trống")
    private BigDecimal amount;

    private String reason;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classroomId;
}
