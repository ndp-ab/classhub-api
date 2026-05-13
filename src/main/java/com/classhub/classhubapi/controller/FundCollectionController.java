package com.classhub.classhubapi.controller;

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

    // Tạo khoản thu mới + tự động tạo payment cho tất cả thành viên
    @PostMapping("/collections")
    public ResponseEntity<CollectionResponse> create(
            @Valid @RequestBody CreateCollectionRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        CollectionResponse response = fundCollectionService.createCollection(request, userId);
        return ResponseEntity.ok(response);
    }

    // Xem danh sách tất cả khoản thu của 1 lớp
    @GetMapping("/collections/{classroomId}")
    public ResponseEntity<List<CollectionResponse>> getByClassroom(
            @PathVariable Long classroomId) {
        return ResponseEntity.ok(fundCollectionService.getCollectionsByClassroom(classroomId));
    }

    // Xem danh sách ai đã đóng / chưa đóng của 1 khoản thu (Admin)
    @GetMapping("/collections/{collectionId}/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @PathVariable Long collectionId) {
        return ResponseEntity.ok(fundCollectionService.getPaymentsByCollection(collectionId));
    }

    // Admin xác nhận 1 sinh viên đã đóng tiền
    @PutMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<PaymentResponse> confirm(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(fundCollectionService.confirmPayment(paymentId));
    }

    // Sinh viên xem nợ cá nhân trong 1 lớp
    @GetMapping("/payments/my/{classroomId}")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @PathVariable Long classroomId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(fundCollectionService.getMyPayments(userId, classroomId));
    }

    // Sinh viên lấy QR chuyển khoản — paymentCode tạo 1 lần, lưu DB
    @GetMapping("/payments/{paymentId}/qr")
    public ResponseEntity<QrResponse> getQr(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(fundCollectionService.generateQr(paymentId));
    }

    // Flutter polling mỗi 5s để biết admin đã xác nhận chưa
    @GetMapping("/payments/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getStatus(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(fundCollectionService.getPaymentStatus(paymentId));
    }
}

