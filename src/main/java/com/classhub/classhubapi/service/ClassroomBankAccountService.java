package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.ClassroomBankAccountResponse;
import com.classhub.classhubapi.dto.UpdateClassroomBankAccountRequest;
import com.classhub.classhubapi.entity.Classroom;
import com.classhub.classhubapi.entity.ClassroomBankAccount;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.ClassroomBankAccountRepository;
import com.classhub.classhubapi.repository.ClassroomRepository;
import com.classhub.classhubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomBankAccountService {

    private final ClassroomBankAccountRepository bankAccountRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    // === XEM TÀI KHOẢN HIỆN TẠI === (Member có thể xem)
    public ClassroomBankAccountResponse getBankAccount(Long classroomId, Long userId) {
        authorizationService.requireMember(userId, classroomId);

        ClassroomBankAccount account = bankAccountRepository
                .findByClassroomIdAndActiveTrue(classroomId)
                .orElseThrow(() -> new BadRequestException(
                        "Lớp chưa cấu hình tài khoản nhận tiền. Vui lòng liên hệ Admin."));

        return toResponse(account);
    }

    // === XEM LỊCH SỬ TÀI KHOẢN === (Chỉ Admin)
    public List<ClassroomBankAccountResponse> getBankAccountHistory(Long classroomId, Long userId) {
        authorizationService.requireAdmin(userId, classroomId);

        return bankAccountRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // === TẠO/CẬP NHẬT TÀI KHOẢN === (Chỉ Admin)
    // Logic: Deactivate tài khoản cũ + Tạo tài khoản mới
    @Transactional
    public ClassroomBankAccountResponse upsertBankAccount(
            Long classroomId, Long userId, UpdateClassroomBankAccountRequest request) {

        authorizationService.requireAdmin(userId, classroomId);

        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // B1: Tắt tài khoản cũ (nếu có)
        bankAccountRepository.findByClassroomIdAndActiveTrue(classroomId)
                .ifPresent(oldAccount -> {
                    oldAccount.setActive(false);
                    bankAccountRepository.save(oldAccount);
                });

        // B2: Tạo tài khoản mới
        ClassroomBankAccount newAccount = ClassroomBankAccount.builder()
                .classroom(classroom)
                .bankBin(request.getBankBin())
                .bankName(request.getBankName())
                .accountNo(request.getAccountNo())
                .accountName(request.getAccountName())
                .active(true)
                .note(request.getNote())
                .createdBy(user)
                .build();

        bankAccountRepository.save(newAccount);
        return toResponse(newAccount);
    }

    // === Helper ===
    private ClassroomBankAccountResponse toResponse(ClassroomBankAccount account) {
        return ClassroomBankAccountResponse.builder()
                .id(account.getId())
                .bankBin(account.getBankBin())
                .bankName(account.getBankName())
                .accountNo(account.getAccountNo())
                .accountName(account.getAccountName())
                .active(account.getActive())
                .note(account.getNote())
                .createdByName(account.getCreatedBy().getFullName())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
