package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.dto.ClassroomResponse;
import com.classhub.classhubapi.dto.CreateClassroomRequest;
import com.classhub.classhubapi.dto.JoinClassroomRequest;
import com.classhub.classhubapi.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor

public class ClassroomController {

    private final ClassroomService classroomService;

    // Tạo lớp mới — Ban cán sự gọi API này
    @PostMapping("/create")
    public ResponseEntity<ClassroomResponse> create(
            @Valid @RequestBody CreateClassroomRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(classroomService.createClassroom(request, userId));
    }

    // Join lớp bằng invite code — Sinh viên gọi API này
    @PostMapping("/join")
    public ResponseEntity<ClassroomResponse> join(
            @Valid @RequestBody JoinClassroomRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(classroomService.joinClassroom(request.getInviteCode(), userId));
    }

    // Xem danh sách lớp của mình
    @GetMapping("/my")
    public ResponseEntity<List<ClassroomResponse>> myClassrooms(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(classroomService.getMyClassrooms(userId));
    }
}