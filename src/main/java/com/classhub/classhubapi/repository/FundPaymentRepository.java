package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.FundPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FundPaymentRepository extends JpaRepository<FundPayment, Long> {

    // Lấy danh sách ai đã đóng/chưa đóng của 1 khoản thu
    List<FundPayment> findByFundCollectionId(Long collectionId);

    // Lấy nợ cá nhân của 1 sinh viên trong 1 lớp
    List<FundPayment> findByUserIdAndFundCollection_ClassroomId(Long userId, Long classroomId);

    // Kiểm tra đã tạo payment cho sinh viên này chưa
    boolean existsByUserIdAndFundCollectionId(Long userId, Long collectionId);

    // Tính tổng tiền đã thu (xác nhận xong)
    BigDecimal sumAmountByFundCollectionIdAndConfirmedByAdminTrue(Long collectionId);
}