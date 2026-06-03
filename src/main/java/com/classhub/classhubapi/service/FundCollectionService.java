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
import com.classhub.classhubapi.exception.ForbiddenException;
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
    private final AuthorizationService authorizationService;

    // === TẠO KHOẢN THU === (Admin)
    @Transactional
    public CollectionResponse createCollection(CreateCollectionRequest request, Long userId) {
        // B2: chỉ Admin của lớp này mới được tạo
        authorizationService.requireAdmin(userId, request.getClassroomId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));
        var classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        FundCollection collection = FundCollection.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .classroom(classroom)
                .createdBy(user)
                .deadline(request.getDeadline())
                .build();
        fundCollectionRepository.save(collection);

        // Tự động tạo payment cho tất cả thành viên
        List<ClassMember> members = classMemberRepository.findByClassroomId(request.getClassroomId());
        for (ClassMember member : members) {
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
    public List<CollectionResponse> getCollectionsByClassroom(Long classroomId, Long userId) {
        authorizationService.requireMember(userId, classroomId);

        List<FundCollection> collections = fundCollectionRepository.findByClassroomId(classroomId);
        return collections.stream().map(collection -> {
            List<FundPayment> payments = fundPaymentRepository.findByFundCollectionId(collection.getId());
            int totalMembers = payments.size();
            int paidCount = (int) payments.stream().filter(FundPayment::isConfirmedByAdmin).count();
            return toCollectionResponse(collection, totalMembers, paidCount);
        }).collect(Collectors.toList());
    }

    // === ADMIN XEM AI ĐÃ ĐÓNG / CHƯA ĐÓNG ===
    public List<PaymentResponse> getPaymentsByCollection(Long collectionId, Long userId) {
        FundCollection collection = fundCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new BadRequestException("Khoản thu không tồn tại"));
        // Phải là Admin của lớp chứa khoản thu này
        authorizationService.requireAdmin(userId, collection.getClassroom().getId());

        return fundPaymentRepository.findByFundCollectionId(collectionId).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // === SINH VIÊN XEM NỢ CÁ NHÂN ===
    public List<PaymentResponse> getMyPayments(Long userId, Long classroomId) {
        authorizationService.requireMember(userId, classroomId);

        return fundPaymentRepository
                .findByUserIdAndFundCollection_ClassroomId(userId, classroomId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    // === MEMBER BÁO ĐÃ CHUYỂN KHOẢN === (GP1)
    // Member tự bấm "Tôi đã chuyển khoản" sau khi CK qua app ngân hàng.
    // Chỉ chủ payment được gọi. Idempotent: nếu đã báo trước đó thì báo lỗi.
    @Transactional
    public PaymentResponse markPaymentAsPaid(Long paymentId, Long userId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn chỉ có thể báo chuyển khoản cho khoản đóng của chính mình");
        }

        if (payment.isConfirmedByAdmin()) {
            throw new BadRequestException("Khoản này đã được Admin xác nhận, không cần báo lại");
        }

        if (payment.isPaid()) {
            throw new BadRequestException("Bạn đã báo chuyển khoản trước đó, vui lòng chờ Admin xác nhận");
        }

        payment.setPaid(true);                          // GP1: isPaid giờ = "Member đã báo CK"
        payment.setMarkedPaidAt(LocalDateTime.now());
        fundPaymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    // === ADMIN XÁC NHẬN ĐÃ ĐÓNG TIỀN === (B3 + GP1)
    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, Long adminUserId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));

        // B2: admin phải thuộc lớp chứa payment này
        Long classroomId = payment.getFundCollection().getClassroom().getId();
        authorizationService.requireAdmin(adminUserId, classroomId);

        // B3 idempotency: chặn confirm 2 lần
        if (payment.isConfirmedByAdmin()) {
            throw new BadRequestException("Khoản thu này đã được xác nhận");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        // Trường hợp Member chưa báo (vd nộp tiền mặt) — Admin vẫn xác nhận được.
        // Đảm bảo isPaid=true để invariant "confirmed implies paid" luôn đúng.
        if (!payment.isPaid()) {
            payment.setPaid(true);
            payment.setMarkedPaidAt(LocalDateTime.now());
        }
        payment.setConfirmedByAdmin(true);
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(admin); // B3: lưu ai xác nhận

        fundPaymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    // === SINH QR === (chỉ chủ payment được xem)
    @Transactional
    public QrResponse generateQr(Long paymentId, Long userId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));
        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn chỉ có thể xem QR của khoản đóng của chính mình");
        }

        if (payment.getPaymentCode() == null) {
            String code = String.format("QUY%d-SV%d-%d",
                    payment.getFundCollection().getId(),
                    payment.getUser().getId(),
                    Instant.now().toEpochMilli());
            payment.setPaymentCode(code);
            fundPaymentRepository.save(payment);
        }

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

    // === POLLING STATUS === (chỉ chủ payment được xem)
    public PaymentStatusResponse getPaymentStatus(Long paymentId, Long userId) {
        FundPayment payment = fundPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Bản ghi thanh toán không tồn tại"));
        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn chỉ có thể xem trạng thái thanh toán của chính mình");
        }

        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .status(computeStatus(payment))
                .markedPaid(payment.isPaid())
                .markedPaidAt(payment.getMarkedPaidAt())
                .confirmedByAdmin(payment.isConfirmedByAdmin())
                .paidAt(payment.getPaidAt())
                .paymentCode(payment.getPaymentCode())
                .isPaid(payment.isConfirmedByAdmin()) // backward compat
                .build();
    }

    // === Helpers ===
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

    private PaymentResponse toPaymentResponse(FundPayment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser().getId())
                .fullName(payment.getUser().getFullName())
                .collectionTitle(payment.getFundCollection().getTitle())
                .amount(payment.getFundCollection().getAmount())
                .deadline(payment.getFundCollection().getDeadline())
                .markedPaid(payment.isPaid())                                       // GP1
                .markedPaidAt(payment.getMarkedPaidAt())                            // GP1
                .confirmedByAdmin(payment.isConfirmedByAdmin())
                .paidAt(payment.getPaidAt())
                .confirmedByName(payment.getConfirmedBy() != null
                        ? payment.getConfirmedBy().getFullName() : null)
                .status(computeStatus(payment))                                     // GP1
                .isPaid(payment.isConfirmedByAdmin())                               // backward compat
                .build();
    }

    // GP1: compute status enum cho FE dễ render
    private String computeStatus(FundPayment payment) {
        if (payment.isConfirmedByAdmin()) return "CONFIRMED";
        if (payment.isPaid()) return "PENDING_VERIFICATION";
        return "UNPAID";
    }
}
