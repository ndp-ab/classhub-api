package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    private String reason;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classroomId;
}
