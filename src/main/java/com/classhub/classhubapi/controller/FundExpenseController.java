package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.dto.CreateExpenseRequest;
import com.classhub.classhubapi.dto.ExpenseResponse;
import com.classhub.classhubapi.service.FundExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fund/expenses")
@RequiredArgsConstructor
public class FundExpenseController {

    private final FundExpenseService fundExpenseService;

    // POST /api/fund/expenses — Admin tạo khoản chi
    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @Valid @RequestBody CreateExpenseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(fundExpenseService.createExpense(request, userId));
    }

    // GET /api/fund/expenses/{classroomId} — Xem danh sách khoản chi của lớp
    @GetMapping("/{classroomId}")
    public ResponseEntity<List<ExpenseResponse>> getByClassroom(
            @PathVariable Long classroomId) {
        return ResponseEntity.ok(fundExpenseService.getExpensesByClassroom(classroomId));
    }
}