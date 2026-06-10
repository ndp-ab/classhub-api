package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banks", uniqueConstraints = @UniqueConstraint(name = "uk_banks_bin", columnNames = "bin"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vietqr_id")
    private Integer vietQrId;

    @Column(nullable = false, length = 6)
    private String bin;

    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(length = 1000)
    private String logo;

    @Builder.Default
    @Column(name = "transfer_supported", nullable = false)
    private Boolean transferSupported = false;

    @Builder.Default
    @Column(name = "lookup_supported", nullable = false)
    private Boolean lookupSupported = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
