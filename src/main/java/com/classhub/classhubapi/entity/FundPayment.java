package com.classhub.classhubapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fund_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private FundCollection fundCollection;

    @Builder.Default
    private boolean isPaid = false;
    @Builder.Default
    private boolean confirmedByAdmin = false;
    private LocalDateTime paidAt;

    // Mã duy nhất mỗi lần mở QR: "QUY{collectionId}-SV{userId}-{timestamp}"
    // Dùng để admin đối soát với sao kê ngân hàng
    @Column(name = "payment_code")
    private String paymentCode;

}
