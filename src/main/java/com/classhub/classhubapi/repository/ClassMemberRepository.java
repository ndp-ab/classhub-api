package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);

    List<ClassMember> findByUserId(Long userId);

    //lấy all thành viên trong lớp
    List<ClassMember> findByClassroomId(Long classroomId);

    // Dùng trong AuthorizationService.requireAdmin để check role
    Optional<ClassMember> findByUserIdAndClassroomId(Long userId, Long classroomId);
}