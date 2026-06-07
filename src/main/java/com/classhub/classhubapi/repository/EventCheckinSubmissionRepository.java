package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.CheckinSubmissionStatus;
import com.classhub.classhubapi.entity.EventCheckinSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventCheckinSubmissionRepository extends JpaRepository<EventCheckinSubmission, Long> {

    Optional<EventCheckinSubmission> findTopByEventIdAndUserIdOrderBySubmittedAtDesc(Long eventId, Long userId);

    List<EventCheckinSubmission> findByEventIdOrderBySubmittedAtDesc(Long eventId);

    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, CheckinSubmissionStatus status);
}
