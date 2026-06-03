# QR Payment Flow - ClassHub Backend

Tài liệu này mô tả luồng QR/VietQR theo code backend hiện tại.

## Tóm tắt

Backend không gọi API VietQR và không cần API key. Backend chỉ ghép URL ảnh QR từ:

- `vietqr.bank-bin`
- `vietqr.account-no`
- `vietqr.account-name`
- `vietqr.template`
- `FundCollection.amount`
- `FundPayment.paymentCode`

Ảnh QR được Flutter tải trực tiếp từ `img.vietqr.io`.

Luồng hiện tại là bán tự động:

1. Admin tạo khoản thu.
2. Backend tự sinh payment cho từng member.
3. Member mở QR để chuyển khoản.
4. Member bấm "Tôi đã chuyển khoản".
5. Admin đối chiếu sao kê và xác nhận.

## Xác thực

Tất cả endpoint fund dưới đây yêu cầu:

```http
Authorization: Bearer <token>
```

Backend không dùng `X-User-Id`.

## Trạng thái payment

Code hiện tại dùng 2 boolean nhưng expose thêm field `status`.

| `isPaid` | `confirmedByAdmin` | `status` | Ý nghĩa |
|---|---|---|---|
| `false` | `false` | `UNPAID` | Member chưa báo đã chuyển khoản |
| `true` | `false` | `PENDING_VERIFICATION` | Member đã báo chuyển khoản, chờ admin xác nhận |
| `true` | `true` | `CONFIRMED` | Admin đã xác nhận |

Lưu ý: field response `isPaid` được giữ để tương thích cũ và có nghĩa là đã được admin xác nhận. Field member tự báo là `markedPaid`.

## Endpoint chính

### Admin tạo khoản thu

```http
POST /api/fund/collections
Authorization: Bearer <adminToken>
Content-Type: application/json
```

```json
{
  "title": "Quỹ tháng 6",
  "amount": 50000,
  "classroomId": 1,
  "deadline": "2026-06-30"
}
```

Backend:

- Kiểm tra user là ADMIN của lớp.
- Lưu `FundCollection`.
- Tạo `FundPayment` cho tất cả member trong lớp.

### Member lấy QR

```http
GET /api/fund/payments/{paymentId}/qr
Authorization: Bearer <memberToken>
```

Chỉ chủ payment được xem QR.

Response:

```json
{
  "paymentId": 1,
  "qrUrl": "https://img.vietqr.io/image/970415-109875610620-compact2.png?amount=50000&addInfo=QUY1-SV2-1780500000000&accountName=Nguyen+Duy+Phonggg",
  "amount": 50000,
  "paymentCode": "QUY1-SV2-1780500000000",
  "collectionTitle": "Quỹ tháng 6",
  "deadline": "2026-06-30"
}
```

### Member báo đã chuyển khoản

```http
POST /api/fund/payments/{paymentId}/mark-paid
Authorization: Bearer <memberToken>
```

Chỉ chủ payment được gọi. Sau khi gọi:

- `markedPaid = true`
- `markedPaidAt = now`
- `status = PENDING_VERIFICATION`
- `confirmedByAdmin = false`

### Member polling trạng thái

```http
GET /api/fund/payments/{paymentId}/status
Authorization: Bearer <memberToken>
```

Response khi chờ xác nhận:

```json
{
  "paymentId": 1,
  "status": "PENDING_VERIFICATION",
  "markedPaid": true,
  "markedPaidAt": "2026-06-03T20:15:00",
  "confirmedByAdmin": false,
  "paidAt": null,
  "paymentCode": "QUY1-SV2-1780500000000",
  "isPaid": false
}
```

Response sau khi admin xác nhận:

```json
{
  "paymentId": 1,
  "status": "CONFIRMED",
  "markedPaid": true,
  "markedPaidAt": "2026-06-03T20:15:00",
  "confirmedByAdmin": true,
  "paidAt": "2026-06-03T20:20:00",
  "paymentCode": "QUY1-SV2-1780500000000",
  "isPaid": true
}
```

Flutter nên polling cho đến khi nhận `CONFIRMED`.

### Admin xác nhận payment

```http
PUT /api/fund/payments/{paymentId}/confirm
Authorization: Bearer <adminToken>
```

Backend:

- Kiểm tra admin thuộc lớp chứa payment.
- Chặn confirm lại nếu `confirmedByAdmin = true`.
- Nếu member chưa bấm `mark-paid`, backend vẫn set `isPaid = true` để giữ invariant confirmed implies paid.
- Set `confirmedByAdmin = true`, `paidAt = now`, `confirmedBy = admin`.

## DTO liên quan

### `PaymentStatusResponse`

```java
private Long paymentId;
private String status; // UNPAID | PENDING_VERIFICATION | CONFIRMED
private boolean markedPaid;
private LocalDateTime markedPaidAt;
private boolean confirmedByAdmin;
private LocalDateTime paidAt;
private String paymentCode;
private boolean isPaid;
```

### `PaymentResponse`

```java
private Long id;
private Long userId;
private String fullName;
private String collectionTitle;
private BigDecimal amount;
private LocalDate deadline;
private boolean markedPaid;
private LocalDateTime markedPaidAt;
private boolean confirmedByAdmin;
private LocalDateTime paidAt;
private String confirmedByName;
private String status;
private boolean isPaid;
```

## Vì sao backend kiểm soát `amount`?

Flutter không truyền amount vào QR endpoint. Backend lấy số tiền từ `FundCollection.amount` để tránh việc client tự sửa amount trên URL QR.

## Cấu hình

`application.properties` cần có:

```properties
vietqr.bank-bin=970415
vietqr.account-no=109875610620
vietqr.account-name=Nguyen Duy Phonggg
vietqr.template=compact2
```

Trước demo nên kiểm tra lại số tài khoản và tên tài khoản.

## Chưa có trong backend hiện tại

- Tự động xác nhận từ webhook ngân hàng.
- OCR sao kê.
- QR hết hạn.
- `confirm-all`.
