package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sự kiện nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Sinh viên nào đăng ký tham gia
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Admin đã check-in chưa (mặc định false)
    @Builder.Default
    @Column(name = "checked_in")
    private boolean checkedIn = false;

    // Thời điểm check-in (null nếu chưa check-in)
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    // Audit trail (B4): admin nào đã check-in sinh viên này. null khi chưa check-in.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    // Thời điểm đăng ký tham gia
    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;
}
