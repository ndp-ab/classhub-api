package com.classhub.classhubapi.repository;

import com.classhub.classhubapi.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Lấy tất cả sự kiện của 1 lớp, sắp xếp theo thời gian sự kiện mới nhất trước
    List<Event> findByClassroomIdOrderByEventTimeDesc(Long classroomId);
}
