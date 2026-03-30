package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.ClassroomResponse;
import com.classhub.classhubapi.dto.CreateClassroomRequest;
import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.entity.Classroom;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.repository.ClassMemberRepository;
import com.classhub.classhubapi.repository.ClassroomRepository;
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
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

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
        // Tìm lớp theo invite code
        Classroom classroom = classroomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Mã tham gia không hợp lệ"));

        // Kiểm tra user đã join lớp này chưa
        if (classMemberRepository.existsByUserIdAndClassroomId(userId, classroom.getId())) {
            throw new RuntimeException("Bạn đã tham gia lớp này rồi");
        }

        // Thêm user vào lớp với role MEMBER
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        ClassMember member = ClassMember.builder()
                .user(user)
                .classroom(classroom)
                .role(ClassMember.Role.MEMBER)
                .build();

        classMemberRepository.save(member);

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
}