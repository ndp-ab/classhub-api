package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);

    List<ClassMember> findByUserId(Long userId);
}