package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên sự kiện, VD: "Họp lớp tháng 5", "Tổng kết cuối năm"
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Mô tả chi tiết (nullable — không bắt buộc)
    @Column(columnDefinition = "TEXT")
    private String description;

    // Địa điểm tổ chức
    private String location;

    // Thời gian bắt đầu sự kiện
    @NotNull
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    // Sự kiện thuộc lớp nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    // Admin nào tạo sự kiện
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
