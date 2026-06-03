package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    // Kiểm tra user đã đăng ký sự kiện này chưa
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    // Lấy danh sách tất cả người đăng ký 1 sự kiện
    List<EventParticipant> findByEventId(Long eventId);

    // Lấy danh sách sự kiện user đã đăng ký (trong 1 lớp cụ thể)
    List<EventParticipant> findByUserIdAndEvent_ClassroomId(Long userId, Long classroomId);

    // Tìm bản ghi participant cụ thể
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("""
            SELECT p.event.id AS eventId,
                   COUNT(p.id) AS volunteerCount,
                   SUM(CASE WHEN p.checkedIn = true THEN 1 ELSE 0 END) AS checkedInCount
            FROM EventParticipant p
            WHERE p.event.id IN :eventIds
            GROUP BY p.event.id
            """)
    List<EventParticipantCount> countByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventParticipantCount {
        Long getEventId();

        long getVolunteerCount();

        long getCheckedInCount();
    }
}
