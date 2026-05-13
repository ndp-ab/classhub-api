# QR Payment Flow — ClassHub

> **Feature:** Tạo mã QR VietQR để sinh viên chuyển khoản đóng quỹ lớp.  
> **Stack:** Flutter (hiển thị) → Spring Boot (generate) → VietQR (ảnh QR)

---

## 1. Tổng quan feature

### Feature này dùng để làm gì

Khi admin tạo khoản thu, hệ thống tự tạo một bản ghi `FundPayment` cho mỗi sinh viên trong lớp. Thay vì yêu cầu sinh viên tự nhớ số tài khoản và nội dung chuyển khoản, feature này cho phép:

- Sinh viên bấm vào khoản nợ → App hiện mã QR chuẩn VietQR
- Sinh viên quét QR bằng app ngân hàng → tự điền số tiền, nội dung chuyển khoản
- Flutter polling backend mỗi 5 giây → khi admin xác nhận → màn hình chuyển sang "Đã đóng ✅"

### User flow tổng quát

```
[Admin]                          [Sinh viên]
   │                                  │
   ├─ Tạo khoản thu "Quỹ T5"          │
   │   → hệ thống tạo N payments      │
   │                                  │
   │                        Mở app → Tab Quỹ lớp
   │                        Thấy "Quỹ T5 — CHƯA ĐÓNG"
   │                        Bấm "Đóng quỹ"
   │                        App hiện QR + nội dung CK
   │                        Quét QR bằng app ngân hàng
   │                        Chuyển khoản xong
   │                                  │
   ├─ Mở danh sách khoản thu           │
   ├─ Thấy tên sinh viên → Tick ✅     │
   │                                  │
   │                        App tự chuyển → "Đã đóng ✅"
```

---

## 2. Architecture

```
Flutter App
    │
    ├── GET /api/fund/payments/{id}/qr
    │       Lấy qrUrl + paymentCode + amount
    │
    ├── Image.network(qrUrl)
    │       Tải ảnh QR trực tiếp từ VietQR CDN
    │
    └── GET /api/fund/payments/{id}/status  (polling 5s)
            Kiểm tra admin đã xác nhận chưa

Spring Boot Backend
    │
    ├── Đọc config ngân hàng từ application.properties
    ├── Tạo paymentCode duy nhất, lưu DB
    └── Ghép URL: https://img.vietqr.io/image/{bin}-{account}-{template}.png
                        ?amount=50000
                        &addInfo=QUY1-SV2-1747093200000
                        &accountName=Nguyen Van Admin

VietQR CDN (img.vietqr.io)
    │
    └── Trả về ảnh PNG chứa mã QR chuẩn Napas 247
        → Flutter dùng Image.network() để hiển thị
```

**Điểm quan trọng:** Backend không gọi API VietQR. Backend chỉ ghép URL — ảnh QR được tải trực tiếp từ CDN của VietQR khi Flutter render. Không cần API key.

---

## 3. Sequence Flow

```
Sinh viên bấm "Đóng quỹ"
    │
    ▼
Flutter: GET /api/fund/payments/{paymentId}/qr
    │
    ▼
Backend: findById(paymentId)
    │
    ├─ paymentCode == null?
    │       YES → tạo "QUY{collId}-SV{userId}-{timestamp}", lưu DB
    │       NO  → dùng lại code cũ (idempotent)
    │
    └─ Ghép VietQR URL với amount, addInfo, accountName
    │
    ▼
Backend trả về:
{
  "qrUrl": "https://img.vietqr.io/image/...",
  "amount": 50000,
  "paymentCode": "QUY1-SV2-1747093200000",
  "collectionTitle": "Quỹ tháng 5",
  "deadline": "2026-05-31"
}
    │
    ▼
Flutter render:
  - Image.network(qrUrl)  → hiện ảnh QR
  - Text(paymentCode)     → sinh viên thấy nội dung CK
  - Text(amount)          → hiện số tiền
    │
    ▼
Flutter polling: GET /api/fund/payments/{id}/status mỗi 5s
    │
    ├─ status == "PENDING" → tiếp tục polling
    └─ status == "CONFIRMED" → hiện màn hình thành công, dừng polling

[Song song] Admin bấm confirm trên app:
    PUT /api/fund/payments/{paymentId}/confirm
    → Backend: isPaid=true, confirmedByAdmin=true, paidAt=now()
    → Lần polling tiếp theo của sinh viên nhận "CONFIRMED"
```

---

## 4. Giải thích từng file quan trọng

### Controller — `FundCollectionController.java`

**Vai trò:** Nhận HTTP request, ủy thác cho Service, trả response.

| Endpoint | Input | Output |
|----------|-------|--------|
| `GET /payments/{id}/qr` | `paymentId` (path) | `QrResponse` |
| `GET /payments/{id}/status` | `paymentId` (path) | `PaymentStatusResponse` |
| `PUT /payments/{id}/confirm` | `paymentId` (path) | `PaymentResponse` |

**Dependency:** `FundCollectionService`

---

### Service — `FundCollectionService.java`

**Vai trò:** Toàn bộ business logic — tạo paymentCode, ghép URL, trả status.

| Method | Input | Output | Logic chính |
|--------|-------|--------|-------------|
| `generateQr(paymentId)` | Long | `QrResponse` | Tạo/lấy code, ghép URL |
| `getPaymentStatus(paymentId)` | Long | `PaymentStatusResponse` | Đọc isPaid từ DB |
| `confirmPayment(paymentId)` | Long | `PaymentResponse` | Set isPaid=true, paidAt=now() |

**Dependency:** `FundPaymentRepository`, `@Value` VietQR config

**Điểm quan trọng:** `generateQr` là **idempotent** — gọi nhiều lần vẫn trả cùng `paymentCode`. Code chỉ tạo mới khi `payment.getPaymentCode() == null`.

---

### Repository — `FundPaymentRepository.java`

**Vai trò:** Truy vấn bảng `fund_payments`.

| Method | Dùng ở đâu |
|--------|-----------|
| `findById(id)` | generateQr, getPaymentStatus, confirmPayment |
| `findByFundCollectionId(id)` | Admin xem danh sách ai đóng |
| `findByUserIdAndFundCollection_ClassroomId(...)` | Sinh viên xem nợ cá nhân |

---

### DTOs

| File | Hướng | Các field |
|------|-------|-----------|
| `QrResponse` | Backend → Flutter | `qrUrl`, `amount`, `paymentCode`, `collectionTitle`, `deadline` |
| `PaymentStatusResponse` | Backend → Flutter | `status` ("PENDING"/"CONFIRMED"), `isPaid`, `paidAt`, `paymentCode` |
| `PaymentResponse` | Backend → Flutter/Admin | `id`, `userId`, `fullName`, `isPaid`, `confirmedByAdmin`, `paidAt` |

---

### Entity — `FundPayment.java`

**Vai trò:** Mapping bảng `fund_payments` trong MySQL.

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `id` | Long | PK |
| `user` | `@ManyToOne` User | Sinh viên nào |
| `fundCollection` | `@ManyToOne` FundCollection | Khoản thu nào |
| `isPaid` | boolean | Đã đóng chưa |
| `confirmedByAdmin` | boolean | Admin đã xác nhận chưa |
| `paidAt` | LocalDateTime | Thời điểm xác nhận |
| `paymentCode` | String | Nội dung CK duy nhất — dùng đối soát sao kê |

---

### Config — `application.properties`

```properties
vietqr.bank-bin=970422        # BIN ngân hàng (MB=970422, VCB=970436...)
vietqr.account-no=0123456789  # Số tài khoản nhận tiền
vietqr.account-name=Nguyen Van Admin
vietqr.template=compact2      # Style QR: compact2 | qr_only | print
```

**Quan trọng:** Thay đổi tài khoản ngân hàng chỉ cần sửa file này, không đụng code.

---

## 5. Business Logic quan trọng

### `paymentCode` — Nội dung chuyển khoản

**Format:** `QUY{collectionId}-SV{userId}-{timestamp}`  
**Ví dụ:** `QUY1-SV2-1747093200000`

- `QUY1` → khoản thu ID=1
- `SV2` → sinh viên ID=2  
- `1747...` → timestamp milliseconds (đảm bảo duy nhất)

**Tại sao lưu vào DB?** Để admin đối soát sao kê ngân hàng. Nội dung CK trên sao kê phải khớp với `paymentCode` trong DB mới xác nhận.

**Tại sao idempotent?** Sinh viên có thể bấm "Đóng quỹ" nhiều lần. Cùng một `paymentCode` đảm bảo không gây nhầm lẫn khi admin đối soát.

---

### `amount` — Số tiền

Lấy từ `FundCollection.amount` — do admin set khi tạo khoản thu. Flutter **không được** tự truyền amount vào QR. Backend kiểm soát hoàn toàn.

---

### `qrUrl` — URL ảnh QR

**Format:**
```
https://img.vietqr.io/image/{bankBin}-{accountNo}-{template}.png
    ?amount={amount}
    &addInfo={paymentCode}
    &accountName={accountName}
```

URL này trả về ảnh PNG trực tiếp — Flutter dùng `Image.network(qrUrl)` để render, không cần parse hay decode gì thêm.

---

### Payment Status — Trạng thái thanh toán

| Field DB | Status API | Ý nghĩa |
|----------|-----------|---------|
| `confirmedByAdmin = false` | `"PENDING"` | Chưa được xác nhận |
| `confirmedByAdmin = true` | `"CONFIRMED"` | Admin đã tick xác nhận |

Flutter polling dựa trên field `status` trong response — khi nhận `"CONFIRMED"` thì dừng polling và show màn hình thành công.

---

## 6. Security Considerations

### Tại sao Flutter không tự generate amount?

Nếu Flutter tự truyền `amount` vào QR URL, sinh viên có thể sửa params để tạo QR với số tiền 0đ. Backend kiểm soát amount bằng cách lấy trực tiếp từ `FundCollection.amount` trong DB — sinh viên không thể can thiệp.

### Tại sao backend phải kiểm soát QR content?

`paymentCode` được tạo và lưu DB bởi backend. Nếu để Flutter tự tạo, sinh viên có thể giả mạo nội dung CK để qua được bước đối soát của admin. Với cách hiện tại:

```
Ngân hàng sao kê: "QUY1-SV2-1747093200000"  ← từ thực tế chuyển khoản
DB fund_payments:  paymentCode = "QUY1-SV2-1747093200000" ← từ backend tạo
→ Khớp → Admin xác nhận an toàn
```

### Giới hạn hiện tại

- `X-User-Id` header chưa được verify qua JWT → cần upgrade JWT filter sau
- API confirm payment chưa kiểm tra role ADMIN → bất kỳ ai biết paymentId đều có thể confirm

---

## 7. Cách Debug Feature Này

### Bước 1: Kiểm tra config ngân hàng

Đảm bảo `application.properties` có đủ 4 field VietQR. Thiếu sẽ báo lỗi khởi động:
```
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'vietqr.bank-bin'
```

### Bước 2: Test theo thứ tự

```
1. POST /api/auth/register          → lấy userId
2. POST /api/classrooms/create      → lấy classroomId (Header: X-User-Id)
3. POST /api/classrooms/join        → thêm member vào lớp
4. POST /api/fund/collections       → lấy collectionId, kiểm tra totalMembers > 0
5. GET  /api/fund/collections/{id}/payments → lấy paymentId của sinh viên
6. GET  /api/fund/payments/{id}/qr  → kiểm tra qrUrl hợp lệ
7. Paste qrUrl vào browser          → phải thấy ảnh QR
8. PUT  /api/fund/payments/{id}/confirm → xác nhận
9. GET  /api/fund/payments/{id}/status  → phải thấy status: "CONFIRMED"
```

### Bước 3: Kiểm tra qrUrl

Paste `qrUrl` từ response vào browser. Nếu thấy ảnh QR → URL đúng. Nếu báo lỗi:
- Kiểm tra `bankBin` có đúng không (tra tại `https://api.vietqr.io/v2/banks`)
- Kiểm tra `amount` không có ký tự lạ (dùng `toPlainString()`, không dùng `toString()`)

### Response mẫu — `/qr`

```json
{
  "paymentId": 2,
  "qrUrl": "https://img.vietqr.io/image/970422-0123456789-compact2.png?amount=50000&addInfo=QUY1-SV2-1747093200000&accountName=Nguyen+Van+Admin",
  "amount": 50000,
  "paymentCode": "QUY1-SV2-1747093200000",
  "collectionTitle": "Quỹ tháng 5",
  "deadline": "2026-05-31"
}
```

### Response mẫu — `/status`

```json
// Chưa xác nhận
{ "paymentId": 2, "status": "PENDING", "isPaid": false, "confirmedByAdmin": false, "paidAt": null }

// Đã xác nhận
{ "paymentId": 2, "status": "CONFIRMED", "isPaid": true, "confirmedByAdmin": true, "paidAt": "2026-05-13T00:25:00" }
```

### Log hữu ích

Vì `spring.jpa.show-sql=true` đã bật, mọi câu SQL đều in ra console. Kiểm tra:
- `SELECT` fund_payment có lấy đúng record không
- `UPDATE` payment_code có được set không (sau lần gọi /qr đầu tiên)

---

## 8. Các Cải Tiến Tương Lai

### Auto Payment Detection (không cần admin confirm thủ công)

Tích hợp với API sao kê ngân hàng (nếu ngân hàng hỗ trợ) hoặc dịch vụ bên thứ 3 (SePay, PayOS) để tự động phát hiện giao dịch chứa `paymentCode` và tự confirm.

```
Ngân hàng → Webhook → Backend nhận → Match paymentCode → Auto confirm
```

**Độ phức tạp:** Cao, cần merchant account hoặc API ngân hàng doanh nghiệp.

---

### QR Dynamic (hết hạn sau N phút)

Hiện tại `paymentCode` không có expiry. Có thể thêm trường `qrExpiresAt` vào `FundPayment`:

- QR hết hạn sau 15 phút
- Sinh viên phải bấm "Làm mới QR" để tạo code mới
- Giảm rủi ro reuse code cũ

---

### Webhook Từ VietQR

VietQR cấp Mức 3 (dành cho doanh nghiệp) hỗ trợ webhook callback khi có giao dịch. Backend nhận callback, parse nội dung CK, match `paymentCode`, tự confirm.

**Điều kiện:** Cần đăng ký tài khoản merchant tại VietQR — ngoài phạm vi đồ án.

---

### Expiration Time cho Payment

Thêm field `deadline` vào `FundPayment` (khác với `FundCollection.deadline`). Nếu sinh viên gọi `/qr` sau hạn, trả về lỗi `"Khoản thu đã hết hạn"`.

---

### Batch Confirm (Admin xác nhận nhiều người cùng lúc)

Thêm endpoint:
```
PUT /api/fund/collections/{collectionId}/confirm-all
Body: { "paymentIds": [2, 3, 5, 7] }
```

Admin tích vào nhiều ô cùng lúc thay vì bấm từng người.
