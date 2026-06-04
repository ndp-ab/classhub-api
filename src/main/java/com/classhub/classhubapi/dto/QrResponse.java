package com.classhub.classhubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class QrResponse {
    private Long paymentId;
    private String qrUrl;           // URL ảnh QR từ VietQR
    private BigDecimal amount;      // Số tiền cần đóng
    private String paymentCode;     // Nội dung chuyển khoản duy nhất
    private String collectionTitle; // Tên khoản thu
    private LocalDate deadline;     // Hạn đóng tiền
    
    // Thông tin tài khoản nhận tiền (từ ClassroomBankAccount)
    private String bankName;        // Tên ngân hàng
    private String accountNo;       // Số tài khoản
    private String accountName;     // Tên chủ tài khoản
}
