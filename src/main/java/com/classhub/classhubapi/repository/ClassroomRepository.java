package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    Optional<Classroom> findByInviteCode(String inviteCode);
}