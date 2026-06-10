package com.classhub.classhubapi.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomBankAccountResponse {

    private Long id;
    private String bankBin;
    private String bankName;
    private String shortName;
    private String accountNo;
    private String accountName;
    private Boolean active;
    private String note;
    private String createdByName;  // Tên Admin đã tạo/cập nhật
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
