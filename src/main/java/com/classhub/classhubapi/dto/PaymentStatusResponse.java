package com.classhub.classhubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentStatusResponse {
    private Long paymentId;

    // "UNPAID" | "PENDING_VERIFICATION" | "CONFIRMED"
    private String status;

    private boolean markedPaid;            // Member đã báo CK chưa
    private LocalDateTime markedPaidAt;

    private boolean confirmedByAdmin;
    private LocalDateTime paidAt;

    private String paymentCode;            // Để Flutter hiển thị nội dung CK

    // Backward compat: isPaid nghĩa cũ = đã được Admin xác nhận
    private boolean isPaid;
}
