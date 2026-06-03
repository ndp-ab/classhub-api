package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.config.SecurityUtil;
import com.classhub.classhubapi.dto.CollectionResponse;
import com.classhub.classhubapi.dto.CreateCollectionRequest;
import com.classhub.classhubapi.dto.PaymentResponse;
import com.classhub.classhubapi.dto.PaymentStatusResponse;
import com.classhub.classhubapi.dto.QrResponse;
import com.classhub.classhubapi.service.FundCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fund")
@RequiredArgsConstructor
public class FundCollectionController {

    private final FundCollectionService fundCollectionService;

    // Tạo khoản thu mới (Admin) — auto-sinh payment cho all members
    @PostMapping("/collections")
    public ResponseEntity<CollectionResponse> create(@Valid @RequestBody CreateCollectionRequest request) {
        return ResponseEntity.ok(
                fundCollectionService.createCollection(request, SecurityUtil.currentUserId()));
    }

    // Danh sách khoản thu của 1 lớp (Member của lớp)
    @GetMapping("/collections/{classroomId}")
    public ResponseEntity<List<CollectionResponse>> getByClassroom(@PathVariable Long classroomId) {
        return ResponseEntity.ok(
                fundCollectionService.getCollectionsByClassroom(classroomId, SecurityUtil.currentUserId()));
    }

    // Admin xem ai đã đóng / chưa đóng
    @GetMapping("/collections/{collectionId}/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long collectionId) {
        return ResponseEntity.ok(
                fundCollectionService.getPaymentsByCollection(collectionId, SecurityUtil.currentUserId()));
    }

    // Admin xác nhận đã đóng tiền
    @PutMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable Long paymentId) {
        return ResponseEntity.ok(
                fundCollectionService.confirmPayment(paymentId, SecurityUtil.currentUserId()));
    }

    // GP1: Member tự báo "Tôi đã chuyển khoản" → status PENDING_VERIFICATION
    @PostMapping("/payments/{paymentId}/mark-paid")
    public ResponseEntity<PaymentResponse> markPaid(@PathVariable Long paymentId) {
        return ResponseEntity.ok(
                fundCollectionService.markPaymentAsPaid(paymentId, SecurityUtil.currentUserId()));
    }

    // Sinh viên xem nợ cá nhân
    @GetMapping("/payments/my/{classroomId}")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@PathVariable Long classroomId) {
        return ResponseEntity.ok(
                fundCollectionService.getMyPayments(SecurityUtil.currentUserId(), classroomId));
    }

    // Sinh viên lấy QR (chỉ chủ payment được xem)
    @GetMapping("/payments/{paymentId}/qr")
    public ResponseEntity<QrResponse> getQr(@PathVariable Long paymentId) {
        return ResponseEntity.ok(
                fundCollectionService.generateQr(paymentId, SecurityUtil.currentUserId()));
    }

    // Flutter polling status
    @GetMapping("/payments/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable Long paymentId) {
        return ResponseEntity.ok(
                fundCollectionService.getPaymentStatus(paymentId, SecurityUtil.currentUserId()));
    }
}
