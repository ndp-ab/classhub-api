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

    // Inject 3 repository — cần truy vấn 3 bảng khác nhau
    private final FundExpenseRepository fundExpenseRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    // === TẠO KHOẢN CHI ===
    public ExpenseResponse createExpense(CreateExpenseRequest request, Long userId) {

        // Bước 2: Tìm user và classroom từ database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        // Bước 3: Tạo entity, gán các trường
        FundExpense expense = FundExpense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .reason(request.getReason())
                .classroom(classroom)
                .createdBy(user)
                .build();

        // Bước 4: Lưu vào database
        fundExpenseRepository.save(expense);

        // Bước 5: Chuyển entity thành response trả về Flutter
        return toResponse(expense);
    }

    // === XEM DANH SÁCH KHOẢN CHI CỦA LỚP ===
    public List<ExpenseResponse> getExpensesByClassroom(Long classroomId) {
        List<FundExpense> expenses = fundExpenseRepository.findByClassroomId(classroomId);

        // Chuyển từng entity thành response bằng stream + map
        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // === Helper: chuyển entity → response ===
    // Tách riêng method này vì dùng ở cả 2 chỗ trên, tránh lặp code
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