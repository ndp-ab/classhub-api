package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
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
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.ok(
                fundExpenseService.createExpense(request, SecurityUtil.currentUserId()));
    }

    // GET /api/fund/expenses/{classroomId} — Member xem danh sách
    @GetMapping("/{classroomId}")
    public ResponseEntity<List<ExpenseResponse>> getByClassroom(@PathVariable Long classroomId) {
        return ResponseEntity.ok(
                fundExpenseService.getExpensesByClassroom(classroomId, SecurityUtil.currentUserId()));
    }
}
