package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    Page<NotificationRecipient> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<NotificationRecipient> findByUserIdAndNotification_Classroom_IdOrderByCreatedAtDesc(
            Long userId,
            Long classroomId,
            Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    long countByUserIdAndReadFalseAndNotification_Classroom_Id(Long userId, Long classroomId);

    Optional<NotificationRecipient> findByIdAndUserId(Long id, Long userId);

    List<NotificationRecipient> findByUserIdAndReadFalse(Long userId);

    List<NotificationRecipient> findByUserIdAndReadFalseAndNotification_Classroom_Id(
            Long userId,
            Long classroomId);
}
