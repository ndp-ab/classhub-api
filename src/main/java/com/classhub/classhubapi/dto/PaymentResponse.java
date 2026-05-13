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
public class PaymentResponse {
    private Long id;
    private Long userId;
    private String fullName;       // Họ tên sinh viên
    private String collectionTitle; // Tên khoản thu
    private boolean isPaid;
    private boolean confirmedByAdmin;
    private LocalDateTime paidAt;
}
