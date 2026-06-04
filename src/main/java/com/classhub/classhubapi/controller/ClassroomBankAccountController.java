package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.ClassroomBankAccountResponse;
import com.classhub.classhubapi.dto.UpdateClassroomBankAccountRequest;
import com.classhub.classhubapi.service.ClassroomBankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms/{classroomId}/bank-account")
@RequiredArgsConstructor
public class ClassroomBankAccountController {

    private final ClassroomBankAccountService bankAccountService;

    // GET /api/classrooms/{classroomId}/bank-account
    // Xem tài khoản hiện tại (Member có thể xem)
    @GetMapping
    public ResponseEntity<ClassroomBankAccountResponse> getBankAccount(
            @PathVariable Long classroomId) {
        return ResponseEntity.ok(
                bankAccountService.getBankAccount(classroomId, SecurityUtil.currentUserId()));
    }

    // GET /api/classrooms/{classroomId}/bank-account/history
    // Xem lịch sử thay đổi STK (Chỉ Admin)
    @GetMapping("/history")
    public ResponseEntity<List<ClassroomBankAccountResponse>> getBankAccountHistory(
            @PathVariable Long classroomId) {
        return ResponseEntity.ok(
                bankAccountService.getBankAccountHistory(classroomId, SecurityUtil.currentUserId()));
    }

    // PUT /api/classrooms/{classroomId}/bank-account
    // Tạo/cập nhật tài khoản (Chỉ Admin)
    @PutMapping
    public ResponseEntity<ClassroomBankAccountResponse> upsertBankAccount(
            @PathVariable Long classroomId,
            @Valid @RequestBody UpdateClassroomBankAccountRequest request) {
        return ResponseEntity.ok(
                bankAccountService.upsertBankAccount(classroomId, SecurityUtil.currentUserId(), request));
    }
}
