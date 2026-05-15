package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.CreateExpenseRequest;
import com.classhub.classhubapi.dto.ExpenseResponse;
import com.classhub.classhubapi.entity.Classroom;
import com.classhub.classhubapi.entity.FundExpense;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.ClassroomRepository;
import com.classhub.classhubapi.repository.FundExpenseRepository;
import com.classhub.classhubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundExpenseService {

    private final FundExpenseRepository fundExpenseRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final AuthorizationService authorizationService;

    // === TẠO KHOẢN CHI === (Admin)
    public ExpenseResponse createExpense(CreateExpenseRequest request, Long userId) {
        // B2: chỉ Admin mới được tạo
        authorizationService.requireAdmin(userId, request.getClassroomId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        FundExpense expense = FundExpense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .reason(request.getReason())
                .classroom(classroom)
                .createdBy(user)
                .build();
        fundExpenseRepository.save(expense);
        return toResponse(expense);
    }

    // === XEM DANH SÁCH KHOẢN CHI === (Member của lớp)
    public List<ExpenseResponse> getExpensesByClassroom(Long classroomId, Long userId) {
        authorizationService.requireMember(userId, classroomId);

        return fundExpenseRepository.findByClassroomId(classroomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ExpenseResponse toResponse(FundExpense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .reason(expense.getReason())
                .createdByName(expense.getCreatedBy().getFullName())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
