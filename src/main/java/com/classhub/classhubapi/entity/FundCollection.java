package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên khoản thu, VD: "Quỹ lớp tháng 4"
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Số tiền mỗi người phải đóng — dùng BigDecimal vì double bị lỗi làm tròn khi tính tiền
    @NotNull
    @Column(nullable = false)
    private BigDecimal amount;

    // Khoản thu này thuộc lớp nào — lưu ID, giống cách Classroom lưu createdBy
    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    // Ai tạo khoản thu — chỉ Admin mới được tạo
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // Hạn chót đóng tiền — dùng LocalDate vì chỉ cần ngày, không cần giờ phút
    private LocalDate deadline;

    // Ngày tạo khoản thu — tự động sinh, không cần set thủ công
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}