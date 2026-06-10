package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ N-1 với Classroom (1 lớp có nhiều tài khoản qua các thời kỳ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    // Bank BIN (mã ngân hàng)
    @NotBlank
    @Column(name = "bank_bin", nullable = false, length = 6)
    private String bankBin;

    // Tên ngân hàng (hiển thị cho user)
    @NotBlank
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "short_name", length = 100)
    private String shortName;

    // Số tài khoản
    @NotBlank
    @Column(name = "account_no", nullable = false, length = 20)
    private String accountNo;

    // Tên chủ tài khoản
    @NotBlank
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    // Chỉ 1 tài khoản active=true / 1 classroom
    // Khi admin đổi STK → tắt cũ (active=false) + tạo mới (active=true)
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // Ghi chú (lý do thay đổi STK)
    // VD: "Thủ quỹ cũ tốt nghiệp", "Đổi sang ngân hàng khác"
    @Column(length = 500)
    private String note;

    // Ai tạo/cập nhật tài khoản ngân hàng này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
