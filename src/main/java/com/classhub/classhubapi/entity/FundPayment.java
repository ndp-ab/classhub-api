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

    // Semantic sau GP1 (member self-report):
    //   isPaid           = Member đã bấm "Tôi đã chuyển khoản"
    //   confirmedByAdmin = Admin đã đối chiếu sao kê + xác nhận
    // → 3 trạng thái: UNPAID (cả 2 false) | PENDING_VERIFICATION (isPaid=true, confirmed=false) | CONFIRMED (cả 2 true)
    @Builder.Default
    private boolean isPaid = false;
    @Builder.Default
    private boolean confirmedByAdmin = false;

    // Member báo đã CK lúc nào (null nếu chưa báo)
    @Column(name = "marked_paid_at")
    private LocalDateTime markedPaidAt;

    // Admin xác nhận lúc nào (null nếu chưa xác nhận)
    private LocalDateTime paidAt;

    // Audit trail (B3): admin nào đã xác nhận khoản thu này
    // null khi chưa được xác nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    // Mã duy nhất mỗi lần mở QR: "QUY{collectionId}-SV{userId}-{timestamp}"
    // Dùng để admin đối soát với sao kê ngân hàng
    @Column(name = "payment_code")
    private String paymentCode;

}
