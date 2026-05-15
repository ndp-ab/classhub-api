package com.classhub.classhubapi.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateCollectionRequest {
    @NotBlank(message = "Tiêu đề khoản thu không được để trống")
    private String title;

    @NotNull(message = "Số tiền khoản thu không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classroomId;

    private LocalDate deadline;
}
