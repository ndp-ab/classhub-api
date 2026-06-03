package com.classhub.classhubapi.service;

import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.exception.ForbiddenException;
import com.classhub.classhubapi.repository.ClassMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Gom logic kiểm tra "user có thuộc lớp / có phải admin của lớp" để mọi service
 * Fund / Event / Expense gọi trước khi thao tác.
 *
 * Cách dùng:
 *   authorizationService.requireMember(userId, classroomId);
 *   authorizationService.requireAdmin(userId, classroomId);
 *
 * Mọi vi phạm → ForbiddenException → handler trả 403 JSON.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final ClassMemberRepository classMemberRepository;

    // User phải thuộc lớp (bất kỳ role nào)
    public void requireMember(Long userId, Long classroomId) {
        if (!classMemberRepository.existsByUserIdAndClassroomId(userId, classroomId)) {
            throw new ForbiddenException("Bạn không thuộc lớp này");
        }
    }

    // User phải là ADMIN của lớp
    public void requireAdmin(Long userId, Long classroomId) {
        ClassMember member = classMemberRepository
                .findByUserIdAndClassroomId(userId, classroomId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc lớp này"));
        if (member.getRole() != ClassMember.Role.ADMIN) {
            throw new ForbiddenException("Chỉ Ban cán sự (Admin) được phép thao tác");
        }
    }
}
