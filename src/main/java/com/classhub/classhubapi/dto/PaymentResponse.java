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
    private BigDecimal amount;      // Số tiền (lấy từ FundCollection.amount)
    private LocalDate deadline;     // Hạn đóng (lấy từ FundCollection.deadline)

    // Member đã bấm "Tôi đã chuyển khoản"
    private boolean markedPaid;
    private LocalDateTime markedPaidAt;

    // Admin đã xác nhận
    private boolean confirmedByAdmin;
    private LocalDateTime paidAt;
    private String confirmedByName;

    // Trạng thái rút gọn cho FE dễ dùng:
    // "UNPAID" | "PENDING_VERIFICATION" | "CONFIRMED"
    private String status;

    // Backward compat: giữ field isPaid nghĩa cũ = đã được Admin xác nhận
    // (Code Flutter cũ dùng isPaid để filter)
    private boolean isPaid;
}
