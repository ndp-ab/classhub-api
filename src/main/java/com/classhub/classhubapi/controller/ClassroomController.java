package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.ClassMemberResponse;
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
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping("/create")
    public ResponseEntity<ClassroomResponse> create(@Valid @RequestBody CreateClassroomRequest request) {
        return ResponseEntity.ok(classroomService.createClassroom(request, SecurityUtil.currentUserId()));
    }

    @PostMapping("/join")
    public ResponseEntity<ClassroomResponse> join(@Valid @RequestBody JoinClassroomRequest request) {
        return ResponseEntity.ok(classroomService.joinClassroom(
                request.getInviteCode(), SecurityUtil.currentUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ClassroomResponse>> myClassrooms() {
        return ResponseEntity.ok(classroomService.getMyClassrooms(SecurityUtil.currentUserId()));
    }

    @GetMapping("/{classroomId}/members")
    public ResponseEntity<List<ClassMemberResponse>> getMembers(@PathVariable Long classroomId) {
        return ResponseEntity.ok(classroomService.getMembers(classroomId, SecurityUtil.currentUserId()));
    }
}
