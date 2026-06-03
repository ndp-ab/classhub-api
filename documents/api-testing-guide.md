# API Testing Guide - ClassHub Backend

Tài liệu này khớp với backend hiện tại trong `classhub-api`.

## Xác thực

Chỉ có các endpoint auth là public:

- `POST /api/auth/register`
- `POST /api/auth/login`

Mọi endpoint khác dưới `/api/**` phải gửi JWT:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

Backend không còn nhận `X-User-Id`. User hiện tại được lấy từ JWT qua `SecurityUtil.currentUserId()`.

## Luồng smoke test đề xuất

### 1. Tạo tài khoản admin

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Admin Class",
  "email": "admin@classhub.test",
  "password": "123456"
}
```

Lưu `token` từ response làm `adminToken`.

### 2. Tạo tài khoản member

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Student One",
  "email": "student@classhub.test",
  "password": "123456"
}
```

Lưu `token` từ response làm `memberToken`.

### 3. Admin tạo lớp

```http
POST /api/classrooms/create
Authorization: Bearer <adminToken>
Content-Type: application/json
```

```json
{
  "className": "CNTT K18",
  "faculty": "Công nghệ thông tin",
  "academicYear": "2022-2026"
}
```

Lưu `id` làm `classroomId`, lưu `inviteCode`.

### 4. Member join lớp

```http
POST /api/classrooms/join
Authorization: Bearer <memberToken>
Content-Type: application/json
```

```json
{
  "inviteCode": "<inviteCode>"
}
```

### 5. Lấy danh sách lớp của user hiện tại

```http
GET /api/classrooms/my
Authorization: Bearer <adminToken>
```

## Fund API

### Tạo khoản thu

Admin của lớp mới được tạo khoản thu.

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

Backend tự sinh `FundPayment` cho tất cả member hiện có. Member join sau cũng được sinh payment bổ sung trong `ClassroomService.joinClassroom`.

### Xem danh sách khoản thu của lớp

```http
GET /api/fund/collections/{classroomId}
Authorization: Bearer <adminToken hoặc memberToken>
```

User phải là thành viên lớp.

### Admin xem payment của một khoản thu

```http
GET /api/fund/collections/{collectionId}/payments
Authorization: Bearer <adminToken>
```

Response `PaymentResponse` hiện có các field chính:

```json
{
  "id": 1,
  "userId": 2,
  "fullName": "Student One",
  "collectionTitle": "Quỹ tháng 6",
  "amount": 50000,
  "deadline": "2026-06-30",
  "markedPaid": false,
  "markedPaidAt": null,
  "confirmedByAdmin": false,
  "paidAt": null,
  "confirmedByName": null,
  "status": "UNPAID",
  "isPaid": false
}
```

`status` có 3 giá trị:

- `UNPAID`: member chưa báo đã chuyển khoản.
- `PENDING_VERIFICATION`: member đã bấm "Tôi đã chuyển khoản", admin chưa xác nhận.
- `CONFIRMED`: admin đã xác nhận.

`isPaid` được giữ để tương thích cũ và có nghĩa là đã được admin xác nhận.

### Member báo đã chuyển khoản

Chỉ chủ payment được gọi.

```http
POST /api/fund/payments/{paymentId}/mark-paid
Authorization: Bearer <memberToken>
```

Sau khi gọi thành công, `markedPaid = true`, `status = "PENDING_VERIFICATION"`.

### Admin xác nhận payment

```http
PUT /api/fund/payments/{paymentId}/confirm
Authorization: Bearer <adminToken>
```

Admin phải thuộc lớp chứa payment. API có idempotency guard: confirm lại payment đã xác nhận sẽ trả 400.

### Member xem nợ cá nhân

```http
GET /api/fund/payments/my/{classroomId}
Authorization: Bearer <memberToken>
```

### Member lấy QR

Chỉ chủ payment được xem QR.

```http
GET /api/fund/payments/{paymentId}/qr
Authorization: Bearer <memberToken>
```

Backend ghép URL VietQR từ `FundCollection.amount`, `paymentCode`, và cấu hình `vietqr.*`.

### Polling trạng thái payment

Chỉ chủ payment được xem.

```http
GET /api/fund/payments/{paymentId}/status
Authorization: Bearer <memberToken>
```

Response:

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

### Tạo khoản chi

Admin của lớp mới được tạo khoản chi.

```http
POST /api/fund/expenses
Authorization: Bearer <adminToken>
Content-Type: application/json
```

```json
{
  "title": "Mua nước",
  "amount": 120000,
  "classroomId": 1,
  "reason": "Sinh hoạt lớp"
}
```

### Xem khoản chi của lớp

```http
GET /api/fund/expenses/{classroomId}
Authorization: Bearer <adminToken hoặc memberToken>
```

User phải là thành viên lớp.

## Event API

### Admin tạo sự kiện

```http
POST /api/events
Authorization: Bearer <adminToken>
Content-Type: application/json
```

```json
{
  "title": "Họp lớp tháng 6",
  "description": "Tổng kết hoạt động",
  "location": "Phòng A101",
  "eventTime": "2026-06-20T08:00:00",
  "classroomId": 1
}
```

Admin phải thuộc lớp.

### Xem sự kiện của lớp

```http
GET /api/events/{classroomId}
Authorization: Bearer <adminToken hoặc memberToken>
```

User phải là thành viên lớp.

### Member đăng ký tham gia

```http
POST /api/events/{eventId}/volunteer
Authorization: Bearer <memberToken>
```

User phải thuộc lớp của event. Backend chặn đăng ký trùng.

### Member hủy đăng ký

```http
DELETE /api/events/{eventId}/volunteer
Authorization: Bearer <memberToken>
```

Nếu participant đã check-in thì không được hủy.

### Admin xem participants

```http
GET /api/events/{eventId}/participants
Authorization: Bearer <adminToken>
```

Admin phải thuộc lớp của event.

### Admin check-in

```http
PUT /api/events/{eventId}/checkin/{userId}
Authorization: Bearer <adminToken>
```

Backend lưu `checkedInAt` và `checkedBy`. Response có `checkedByName`.

### Member xem event đã đăng ký

```http
GET /api/events/my/{classroomId}
Authorization: Bearer <memberToken>
```

## Mã lỗi thường gặp

| Status | Khi nào |
|---|---|
| 400 | Request body sai, dữ liệu nghiệp vụ không hợp lệ, duplicate action |
| 401 | Thiếu token hoặc token không hợp lệ |
| 403 | Có token nhưng không thuộc lớp hoặc không phải admin |
| 500 | Lỗi server ngoài dự kiến |

## Checklist endpoint hiện tại

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/classrooms/create` | Bearer |
| POST | `/api/classrooms/join` | Bearer |
| GET | `/api/classrooms/my` | Bearer |
| POST | `/api/fund/collections` | Bearer, admin |
| GET | `/api/fund/collections/{classroomId}` | Bearer, member |
| GET | `/api/fund/collections/{collectionId}/payments` | Bearer, admin |
| PUT | `/api/fund/payments/{paymentId}/confirm` | Bearer, admin |
| POST | `/api/fund/payments/{paymentId}/mark-paid` | Bearer, owner |
| GET | `/api/fund/payments/my/{classroomId}` | Bearer, member |
| GET | `/api/fund/payments/{paymentId}/qr` | Bearer, owner |
| GET | `/api/fund/payments/{paymentId}/status` | Bearer, owner |
| POST | `/api/fund/expenses` | Bearer, admin |
| GET | `/api/fund/expenses/{classroomId}` | Bearer, member |
| POST | `/api/events` | Bearer, admin |
| GET | `/api/events/{classroomId}` | Bearer, member |
| POST | `/api/events/{eventId}/volunteer` | Bearer, member |
| DELETE | `/api/events/{eventId}/volunteer` | Bearer, owner |
| GET | `/api/events/{eventId}/participants` | Bearer, admin |
| PUT | `/api/events/{eventId}/checkin/{userId}` | Bearer, admin |
| GET | `/api/events/my/{classroomId}` | Bearer, member |
