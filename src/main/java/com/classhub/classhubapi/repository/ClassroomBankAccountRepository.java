package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.ClassroomBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomBankAccountRepository extends JpaRepository<ClassroomBankAccount, Long> {

    // Lấy tài khoản ĐANG DÙNG (active=true)
    // → Dùng khi: sinh QR, hiển thị thông tin STK hiện tại
    Optional<ClassroomBankAccount> findByClassroomIdAndActiveTrue(Long classroomId);

    // Lấy TẤT CẢ tài khoản (lịch sử), sắp xếp mới nhất trước
    // → Dùng khi: Admin xem lịch sử thay đổi STK
    List<ClassroomBankAccount> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);

    // Kiểm tra lớp đã có tài khoản active chưa
    // → Dùng khi: validation trước khi tạo khoản thu
    boolean existsByClassroomIdAndActiveTrue(Long classroomId);
}
