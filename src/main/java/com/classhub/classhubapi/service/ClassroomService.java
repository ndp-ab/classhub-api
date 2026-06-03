package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.ClassMemberResponse;
import com.classhub.classhubapi.dto.ClassroomResponse;
import com.classhub.classhubapi.dto.CreateClassroomRequest;
import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.entity.Classroom;
import com.classhub.classhubapi.entity.FundCollection;
import com.classhub.classhubapi.entity.FundPayment;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.ClassMemberRepository;
import com.classhub.classhubapi.repository.ClassroomRepository;
import com.classhub.classhubapi.repository.FundCollectionRepository;
import com.classhub.classhubapi.repository.FundPaymentRepository;
import com.classhub.classhubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    // B6: cần inject để sinh payment bổ sung khi member join muộn
    private final FundCollectionRepository fundCollectionRepository;
    private final FundPaymentRepository fundPaymentRepository;
    private final AuthorizationService authorizationService;

    // === TẠO LỚP ===
    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request, Long userId) {
        // Sinh invite code 6 ký tự từ UUID
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Tạo classroom
        Classroom classroom = Classroom.builder()
                .className(request.getClassName())
                .faculty(request.getFaculty())
                .academicYear(request.getAcademicYear())
                .inviteCode(inviteCode)
                .createdBy(userId)
                .build();

        classroomRepository.save(classroom);

        // Tự động gán người tạo là ADMIN
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        ClassMember member = ClassMember.builder()
                .user(user)
                .classroom(classroom)
                .role(ClassMember.Role.ADMIN)
                .build();

        classMemberRepository.save(member);

        return ClassroomResponse.builder()
                .id(classroom.getId())
                .className(classroom.getClassName())
                .faculty(classroom.getFaculty())
                .academicYear(classroom.getAcademicYear())
                .inviteCode(classroom.getInviteCode())
                .role("ADMIN")
                .build();
    }

    // === JOIN LỚP BẰNG INVITE CODE ===
    @Transactional
    public ClassroomResponse joinClassroom(String inviteCode, Long userId) {
        Classroom classroom = classroomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BadRequestException("Mã tham gia không hợp lệ"));

        if (classMemberRepository.existsByUserIdAndClassroomId(userId, classroom.getId())) {
            throw new BadRequestException("Bạn đã tham gia lớp này rồi");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        ClassMember member = ClassMember.builder()
                .user(user)
                .classroom(classroom)
                .role(ClassMember.Role.MEMBER)
                .build();
        classMemberRepository.save(member);

        // B6: sinh payment bổ sung cho tất cả khoản thu đã tồn tại trong lớp
        // → member join muộn vẫn thấy nợ
        List<FundCollection> existingCollections =
                fundCollectionRepository.findByClassroomId(classroom.getId());
        for (FundCollection collection : existingCollections) {
            if (!fundPaymentRepository.existsByUserIdAndFundCollectionId(userId, collection.getId())) {
                FundPayment payment = FundPayment.builder()
                        .user(user)
                        .fundCollection(collection)
                        .isPaid(false)
                        .confirmedByAdmin(false)
                        .build();
                fundPaymentRepository.save(payment);
            }
        }

        return ClassroomResponse.builder()
                .id(classroom.getId())
                .className(classroom.getClassName())
                .faculty(classroom.getFaculty())
                .academicYear(classroom.getAcademicYear())
                .inviteCode(classroom.getInviteCode())
                .role("MEMBER")
                .build();
    }

    // === XEM DANH SÁCH LỚP CỦA USER ===
    public List<ClassroomResponse> getMyClassrooms(Long userId) {
        List<ClassMember> memberships = classMemberRepository.findByUserId(userId);

        return memberships.stream().map(m -> ClassroomResponse.builder()
                .id(m.getClassroom().getId())
                .className(m.getClassroom().getClassName())
                .faculty(m.getClassroom().getFaculty())
                .academicYear(m.getClassroom().getAcademicYear())
                .inviteCode(m.getClassroom().getInviteCode())
                .role(m.getRole().name())
                .build()
        ).collect(Collectors.toList());
    }

    // === XEM DANH SÁCH THÀNH VIÊN CỦA LỚP ===
    public List<ClassMemberResponse> getMembers(Long classroomId, Long userId) {
        // User phải thuộc lớp mới xem được danh sách thành viên
        authorizationService.requireMember(userId, classroomId);

        List<ClassMember> members = classMemberRepository.findByClassroomId(classroomId);

        return members.stream()
                .map(m -> ClassMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .email(m.getUser().getEmail())
                        .role(m.getRole().name())
                        .joinedAt(m.getJoinedAt())
                        .build()
                )
                .collect(Collectors.toList());
    }
}