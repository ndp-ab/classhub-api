package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.FundCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface FundCollectionRepository extends JpaRepository<FundCollection, Long> {
    // Lấy danh sách khoản thu của 1 lớp
    // → Dùng cho: Sinh viên mở tab "Quỹ lớp" để xem danh sách khoản thu
    List<FundCollection> findByClassroomId(Long classroomId);

    // Kiểm tra khoản thu có tồn tại và thuộc lớp này không
    // → Dùng cho: Xác nhận quyền truy cập (security check)
    boolean existsByIdAndClassroomId(Long id, Long classroomId);
}
