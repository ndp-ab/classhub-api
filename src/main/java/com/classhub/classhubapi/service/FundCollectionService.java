package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.CollectionResponse;
import com.classhub.classhubapi.dto.CreateCollectionRequest;
import com.classhub.classhubapi.dto.PaymentResponse;
import com.classhub.classhubapi.dto.PaymentStatusResponse;
import com.classhub.classhubapi.dto.QrResponse;
import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.entity.FundCollection;
import com.classhub.classhubapi.entity.FundPayment;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundCollectionService {

    // Config từ application.properties
    @Value("${vietqr.bank-bin}")
    private String bankBin;

    @Value("${vietqr.account-no}")
    private String accountNo;

    @Value("${vietqr.account-name}")
    private String accountName;

    @Value("${vietqr.template}")
    private String qrTemplate;

    private final FundCollectionRepository fundCollectionRepository;
    private final FundPaymentRepository fundPaymentRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    // === TẠO KHOẢN THU ===
    // Dùng @Transactional: đảm bảo nếu tạo payment bị lỗi giữa chừng thì rollback hết
    @Transactional
    public CollectionResponse createCollection(CreateCollectionRequest request, Long userId) {
        // Bước 1: Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // Bước 2: Kiểm tra lớp tồn tại
        var classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        // Bước 3: Tạo entity khoản thu và lưu vào DB
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
            // Kiểm tra payment chưa tồn tại (tránh duplicate khi gọi lại)
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

        return toCollectionResponse(collection, members.size(), 0);
    }

    // === XEM DANH SÁCH KHOẢN THU CỦA LỚP ===
    public List<CollectionResponse> getCollectionsByClassroom(Long classroomId) {
        List<FundCollection> collections = fundCollectionRepository.findByClassroomId(classroomId);

        return collections.stream().map(collection -> {
            List<FundPayment> payments = fundPaymentRepository.findByFundCollectionId(collection.getId());
            int totalMembers = payments.size();
            // Đếm số người đã được admin xác nhận đóng tiền
            int paidCount = (int) payments.stream().filter(FundPayment::isConfirmedByAdmin).count();
            return toCollectionResponse(collection, totalMembers, paidCount);
        }).collect(Collectors.toList());
    }

    // === XEM DANH SÁCH AI ĐÃ ĐÓNG / CHƯA ĐÓNG (Admin dùng) ===
    public List<PaymentResponse> getPaymentsByCollection(Long collectionId) {
        // Kiểm tra khoản thu có tồn tại không
        fundCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new BadRequestException("Khoản thu không tồn tại"));

        List<FundPayment> payments = fundPaymentRepository.findByFundCollectionId(collectionId);

        return payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // === XEM NỢ CÁ NHÂN CỦA SINH VIÊN TRONG LỚP ===
    public List<PaymentResponse> getMyPayments(Long userId, Long classroomId) {
        List<FundPayment> payments = fundPaymentRepository
                .findByUserIdAndFundCollection_ClassroomId(userId, classroomId);

        return payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // === ADMIN XÁC NHẬN ĐÃ ĐÓNG TIỀN ===
    @Transactional
    public PaymentResponse confirmPayment(Long paymentId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));

        // Cập nhật trạng thái xác nhận
        payment.setPaid(true);
        payment.setConfirmedByAdmin(true);
        payment.setPaidAt(LocalDateTime.now());

        fundPaymentRepository.save(payment);

        return toPaymentResponse(payment);
    }

    // === SINH QR VIETQR ===
    // paymentCode được tạo 1 lần và lưu DB — lần sau gọi lại trả code cũ
    @Transactional
    public QrResponse generateQr(Long paymentId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));

        // Nếu chưa có paymentCode thì tạo mới, đã có thì dùng lại
        if (payment.getPaymentCode() == null) {
            String code = String.format("QUY%d-SV%d-%d",
                    payment.getFundCollection().getId(),
                    payment.getUser().getId(),
                    Instant.now().toEpochMilli());
            payment.setPaymentCode(code);
            fundPaymentRepository.save(payment);
        }

        // Ghép VietQR URL: https://img.vietqr.io/image/{bin}-{account}-{template}.png
        // .encode() URL-encode query params (space → %20). fromUriString thay fromHttpUrl (Spring 6)
        String qrUrl = UriComponentsBuilder
                .fromUriString(String.format("https://img.vietqr.io/image/%s-%s-%s.png",
                        bankBin, accountNo, qrTemplate))
                .queryParam("amount", payment.getFundCollection().getAmount().toPlainString())
                .queryParam("addInfo", payment.getPaymentCode())
                .queryParam("accountName", accountName)
                .build().encode().toUriString();

        return QrResponse.builder()
                .paymentId(payment.getId())
                .qrUrl(qrUrl)
                .amount(payment.getFundCollection().getAmount())
                .paymentCode(payment.getPaymentCode())
                .collectionTitle(payment.getFundCollection().getTitle())
                .deadline(payment.getFundCollection().getDeadline())
                .build();
    }

    // === POLLING TRẠNG THÁI THANH TOÁN ===
    public PaymentStatusResponse getPaymentStatus(Long paymentId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));

        String status = payment.isConfirmedByAdmin() ? "CONFIRMED" : "PENDING";

        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .status(status)
                .isPaid(payment.isConfirmedByAdmin())
                .confirmedByAdmin(payment.isConfirmedByAdmin())
                .paidAt(payment.getPaidAt())
                .paymentCode(payment.getPaymentCode())
                .build();
    }

    // === Helper: FundCollection entity → CollectionResponse ===
    private CollectionResponse toCollectionResponse(FundCollection collection, int totalMembers, int paidCount) {
        return CollectionResponse.builder()
                .id(collection.getId())
                .title(collection.getTitle())
                .amount(collection.getAmount())
                .deadline(collection.getDeadline())
                .createdByName(collection.getCreatedBy().getFullName())
                .totalMembers(totalMembers)
                .paidCount(paidCount)
                .createdAt(collection.getCreatedAt())
                .build();
    }

    // === Helper: FundPayment entity → PaymentResponse ===
    private PaymentResponse toPaymentResponse(FundPayment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser().getId())
                .fullName(payment.getUser().getFullName())
                .collectionTitle(payment.getFundCollection().getTitle())
                .isPaid(payment.isConfirmedByAdmin()) // isPaid từ góc nhìn đã xác nhận
                .confirmedByAdmin(payment.isConfirmedByAdmin())
                .paidAt(payment.getPaidAt())
                .build();
    }
}

