package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.CreateCollectionRequest;
import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.entity.FundCollection;
import com.classhub.classhubapi.entity.FundPayment;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class FundCollectionService {
    private final FundCollectionRepository fundCollectionRepository;
    private final FundPaymentRepository fundPaymentRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    // === TẠO KHOẢN THU ===
    @Transactional
    public FundCollection createCollection(CreateCollectionRequest request, Long userId) {
        // Bước 1: Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // Bước 2: Kiểm tra lớp tồn tại
        var classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        // Bước 3: Tạo entity khoản thu
        FundCollection collection = FundCollection.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .classroom(classroom)
                .createdBy(user)
                .deadline(request.getDeadline())
                .build();

        fundCollectionRepository.save(collection);

        // Bước 4: Tự động tạo payment cho TẤT CẢ thành viên của lớp
        List<ClassMember> members = classMemberRepository.findByClassroomId(request.getClassroomId());

        for (ClassMember member : members) {
            // Kiểm tra payment chưa tồn tại (tránh duplicate)
            if (!fundPaymentRepository.existsByUserIdAndFundCollectionId(
                    member.getUser().getId(), collection.getId())) {

                FundPayment payment = FundPayment.builder()
                        .user(member.getUser())
                        .fundCollection(collection)
                        .isPaid(false)
                        .confirmedByAdmin(false)
                        .build();

                fundPaymentRepository.save(payment);
            }
        }

        return collection;
    }
}
