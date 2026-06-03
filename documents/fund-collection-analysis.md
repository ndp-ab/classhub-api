# Fund Collection Analysis - Current Backend

Tài liệu này mô tả module quỹ lớp theo code backend hiện tại.

## Flow khoản thu

### Admin tạo khoản thu

1. Client gọi `POST /api/fund/collections` với `Authorization: Bearer <adminToken>`.
2. `FundCollectionController` lấy user hiện tại từ `SecurityUtil.currentUserId()`.
3. `FundCollectionService.createCollection` gọi `authorizationService.requireAdmin`.
4. Backend lưu `FundCollection`.
5. Backend tạo `FundPayment` cho tất cả member trong lớp.

Nếu member join lớp sau khi đã có khoản thu, `ClassroomService.joinClassroom` sẽ tạo payment bổ sung cho member đó.

### Member xem khoản của mình

1. Client gọi `GET /api/fund/payments/my/{classroomId}`.
2. Service gọi `requireMember`.
3. Backend trả danh sách `PaymentResponse` của user hiện tại.

### Member thanh toán qua QR

1. Client gọi `GET /api/fund/payments/{paymentId}/qr`.
2. Service kiểm tra payment thuộc user hiện tại.
3. Backend sinh `paymentCode` nếu chưa có.
4. Backend ghép `qrUrl` VietQR.
5. Member chuyển khoản bằng app ngân hàng.
6. Member gọi `POST /api/fund/payments/{paymentId}/mark-paid`.
7. Payment chuyển sang `PENDING_VERIFICATION`.

### Admin xác nhận

1. Client gọi `GET /api/fund/collections/{collectionId}/payments`.
2. Admin đối chiếu sao kê với `paymentCode`.
3. Client gọi `PUT /api/fund/payments/{paymentId}/confirm`.
4. Service kiểm tra admin thuộc lớp chứa payment.
5. Backend set `confirmedByAdmin`, `paidAt`, `confirmedBy`.
6. Payment chuyển sang `CONFIRMED`.

## Trạng thái payment

| Trạng thái | Điều kiện |
|---|---|
| `UNPAID` | `isPaid = false`, `confirmedByAdmin = false` |
| `PENDING_VERIFICATION` | `isPaid = true`, `confirmedByAdmin = false` |
| `CONFIRMED` | `isPaid = true`, `confirmedByAdmin = true` |

Trong response, `markedPaid` biểu thị member đã báo chuyển khoản. Field `isPaid` response được giữ để tương thích cũ và có nghĩa là đã được admin xác nhận.

## Endpoints

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/api/fund/collections` | ADMIN |
| GET | `/api/fund/collections/{classroomId}` | MEMBER |
| GET | `/api/fund/collections/{collectionId}/payments` | ADMIN |
| PUT | `/api/fund/payments/{paymentId}/confirm` | ADMIN |
| POST | `/api/fund/payments/{paymentId}/mark-paid` | OWNER |
| GET | `/api/fund/payments/my/{classroomId}` | MEMBER |
| GET | `/api/fund/payments/{paymentId}/qr` | OWNER |
| GET | `/api/fund/payments/{paymentId}/status` | OWNER |
| POST | `/api/fund/expenses` | ADMIN |
| GET | `/api/fund/expenses/{classroomId}` | MEMBER |

## Còn thiếu

- Chưa có update/delete khoản thu.
- Chưa có update/delete khoản chi.
- Chưa có thống kê quỹ tổng hợp.
- Chưa có tự động đối soát ngân hàng.
