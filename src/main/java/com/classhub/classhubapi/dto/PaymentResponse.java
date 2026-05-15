package com.classhub.classhubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long userId;
    private String fullName;        // Họ tên sinh viên
    private String collectionTitle; // Tên khoản thu
    private BigDecimal amount;      // Số tiền (lấy từ FundCollection.amount) — Flutter cần để hiển thị
    private LocalDate deadline;     // Hạn đóng (lấy từ FundCollection.deadline)
    private boolean isPaid;
    private boolean confirmedByAdmin;
    private LocalDateTime paidAt;
    private String confirmedByName; // Audit (B3): ai đã xác nhận
}
