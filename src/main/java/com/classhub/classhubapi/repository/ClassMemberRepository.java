package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);

    List<ClassMember> findByUserId(Long userId);

    //lấy all thành viên trong lớp
    List<ClassMember> findByClassroomId(Long classroomId);
}