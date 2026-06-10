package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClassroomBankAccountRequest {

    @NotBlank(message = "Bank BIN không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Bank BIN phải là 6 chữ số")
    private String bankBin;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^\\d{6,20}$", message = "Số tài khoản phải là 6-20 chữ số")
    private String accountNo;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String accountName;

    private String note;  // Optional: lý do thay đổi
}
