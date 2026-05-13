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
    private String status;          // "PENDING" | "CONFIRMED"
    private boolean isPaid;
    private boolean confirmedByAdmin;
    private LocalDateTime paidAt;
    private String paymentCode;     // Để Flutter hiển thị nội dung CK
}
