# API Testing Guide — ClassHub Backend

> **Base URL:** `http://192.168.1.5:8080/api`  
> **Tool:** Postman  
> **Lưu ý:** Thay IP theo máy thực tế (`ipconfig` → IPv4 Address)

---

## Thứ tự test (theo luồng thực tế)

```
1. Auth: Register → Login (lấy userId)
2. Classroom: Create → Join (lấy classroomId)
3. Fund Expense: Create → List
4. Fund Collection: Create → List → Payments → Confirm → My Debts
```

---

## 1. Auth

### 1.1 Đăng ký tài khoản Admin (Ban cán sự)

**POST** `/api/auth/register`

```json
// Body (raw JSON)
{
  "fullName": "Nguyễn Văn Admin",
  "email": "admin@test.com",
  "password": "123456"
}
```

**Response mong đợi (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "fullName": "Nguyễn Văn Admin",
  "email": "admin@test.com"
}
```
> 📌 Lưu lại `userId = 1` → dùng làm header `X-User-Id` cho các request sau

---

### 1.2 Đăng ký tài khoản Member (Sinh viên)

**POST** `/api/auth/register`

```json
{
  "fullName": "Trần Thị Sinh Viên",
  "email": "sinhvien@test.com",
  "password": "123456"
}
```

**Response mong đợi (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 2,
  "fullName": "Trần Thị Sinh Viên",
  "email": "sinhvien@test.com"
}
```
> 📌 Lưu lại `userId = 2`

---

### 1.3 Đăng nhập

**POST** `/api/auth/login`

```json
{
  "email": "admin@test.com",
  "password": "123456"
}
```

**Response mong đợi (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "fullName": "Nguyễn Văn Admin",
  "email": "admin@test.com"
}
```

**Test case lỗi:**
```json
// Sai password → 400
{
  "email": "admin@test.com",
  "password": "sai_mat_khau"
}
// Response: { "message": "Sai mật khẩu" }
```

---

## 2. Classroom

### 2.1 Tạo lớp học

**POST** `/api/classrooms/create`

```
Header: X-User-Id: 1
```
```json
{
  "className": "64KTPM3",
  "faculty": "Công nghệ thông tin",
  "academicYear": "K64"
}
```

**Response mong đợi (200):**
```json
{
  "id": 1,
  "className": "64KTPM3",
  "faculty": "Công nghệ thông tin",
  "academicYear": "K64",
  "inviteCode": "ABC123",
  "role": "ADMIN"
}
```
> 📌 Lưu lại `classroomId = 1` và `inviteCode = "ABC123"`

---

### 2.2 Join lớp (với account sinh viên)

**POST** `/api/classrooms/join`

```
Header: X-User-Id: 2
```
```json
{
  "inviteCode": "ABC123"
}
```

**Response mong đợi (200):**
```json
{
  "id": 1,
  "className": "64KTPM3",
  "role": "MEMBER"
}
```

---

### 2.3 Xem danh sách lớp của tôi

**GET** `/api/classrooms/my`

```
Header: X-User-Id: 1
```

**Response mong đợi (200):**
```json
[
  {
    "id": 1,
    "className": "64KTPM3",
    "faculty": "Công nghệ thông tin",
    "academicYear": "K64",
    "inviteCode": "ABC123",
    "role": "ADMIN"
  }
]
```

---

## 3. Fund — Khoản chi

### 3.1 Tạo khoản chi

**POST** `/api/fund/expenses`

```
Header: X-User-Id: 1
```
```json
{
  "title": "Mua hoa 20/11",
  "amount": 500000,
  "classroomId": 1,
  "reason": "Tặng giáo viên chủ nhiệm"
}
```

**Response mong đợi (200):**
```json
{
  "id": 1,
  "title": "Mua hoa 20/11",
  "amount": 500000,
  "reason": "Tặng giáo viên chủ nhiệm",
  "createdByName": "Nguyễn Văn Admin",
  "createdAt": "2026-05-13T00:00:00"
}
```

---

### 3.2 Xem danh sách khoản chi

**GET** `/api/fund/expenses/1`

**Response mong đợi (200):**
```json
[
  {
    "id": 1,
    "title": "Mua hoa 20/11",
    "amount": 500000,
    "reason": "Tặng giáo viên chủ nhiệm",
    "createdByName": "Nguyễn Văn Admin",
    "createdAt": "2026-05-13T00:00:00"
  }
]
```

---

## 4. Fund — Khoản thu ✅ (Mới implement)

> **Luồng:** Tạo khoản thu → Xem danh sách → Admin xem ai đóng → Admin xác nhận → Sinh viên xem nợ

### 4.1 Tạo khoản thu (Admin)

**POST** `/api/fund/collections`

```
Header: X-User-Id: 1
```
```json
{
  "title": "Quỹ tháng 5",
  "amount": 50000,
  "classroomId": 1,
  "deadline": "2026-05-31"
}
```

**Response mong đợi (200):**
```json
{
  "id": 1,
  "title": "Quỹ tháng 5",
  "amount": 50000,
  "deadline": "2026-05-31",
  "createdByName": "Nguyễn Văn Admin",
  "totalMembers": 2,
  "paidCount": 0,
  "createdAt": "2026-05-13T00:00:00"
}
```
> ✅ `totalMembers: 2` nghĩa là đã tự tạo 2 bản ghi payment (Admin + Member)

**Test case lỗi:**
```json
// Thiếu title → 400
{
  "amount": 50000,
  "classroomId": 1
}
// Response: { "message": "Tiêu đề khoản thu không được để trống" }
```

---

### 4.2 Xem danh sách khoản thu của lớp

**GET** `/api/fund/collections/1`

**Response mong đợi (200):**
```json
[
  {
    "id": 1,
    "title": "Quỹ tháng 5",
    "amount": 50000,
    "deadline": "2026-05-31",
    "createdByName": "Nguyễn Văn Admin",
    "totalMembers": 2,
    "paidCount": 0,
    "createdAt": "2026-05-13T00:00:00"
  }
]
```

---

### 4.3 Admin xem ai đã đóng / chưa đóng

**GET** `/api/fund/collections/1/payments`

**Response mong đợi (200):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "fullName": "Nguyễn Văn Admin",
    "collectionTitle": "Quỹ tháng 5",
    "isPaid": false,
    "confirmedByAdmin": false,
    "paidAt": null
  },
  {
    "id": 2,
    "userId": 2,
    "fullName": "Trần Thị Sinh Viên",
    "collectionTitle": "Quỹ tháng 5",
    "isPaid": false,
    "confirmedByAdmin": false,
    "paidAt": null
  }
]
```
> 📌 Lưu lại `paymentId` (VD: `id = 2`) của sinh viên cần xác nhận

---

### 4.4 Admin xác nhận sinh viên đã đóng tiền

**PUT** `/api/fund/payments/2/confirm`

*(Không cần body)*

**Response mong đợi (200):**
```json
{
  "id": 2,
  "userId": 2,
  "fullName": "Trần Thị Sinh Viên",
  "collectionTitle": "Quỹ tháng 5",
  "isPaid": true,
  "confirmedByAdmin": true,
  "paidAt": "2026-05-13T00:25:00"
}
```

**Test case lỗi:**
```
PUT /api/fund/payments/999/confirm
// Response 400: { "message": "Bản ghi thanh toán không tồn tại" }
```

---

### 4.5 Kiểm tra lại danh sách sau khi xác nhận

**GET** `/api/fund/collections/1/payments`

**Kiểm tra:** `paidCount` trong response của 4.2 phải tăng lên `1` sau khi gọi lại:

**GET** `/api/fund/collections/1`

```json
[
  {
    "id": 1,
    "title": "Quỹ tháng 5",
    "totalMembers": 2,
    "paidCount": 1,     ← tăng từ 0 lên 1 ✅
    ...
  }
]
```

---

### 4.6 Sinh viên xem nợ cá nhân

**GET** `/api/fund/payments/my/1`

```
Header: X-User-Id: 2
```

**Response mong đợi (200):**
```json
[
  {
    "id": 2,
    "userId": 2,
    "fullName": "Trần Thị Sinh Viên",
    "collectionTitle": "Quỹ tháng 5",
    "isPaid": true,
    "confirmedByAdmin": true,
    "paidAt": "2026-05-13T00:25:00"
  }
]
```

---

## 5. Bảng tổng hợp tất cả API

| # | Method | Endpoint | Header | Body | Trạng thái |
|---|--------|----------|--------|------|------------|
| 1 | POST | /api/auth/register | — | fullName, email, password | ✅ |
| 2 | POST | /api/auth/login | — | email, password | ✅ |
| 3 | POST | /api/classrooms/create | X-User-Id | className, faculty, academicYear | ✅ |
| 4 | POST | /api/classrooms/join | X-User-Id | inviteCode | ✅ |
| 5 | GET | /api/classrooms/my | X-User-Id | — | ✅ |
| 6 | POST | /api/fund/expenses | X-User-Id | title, amount, classroomId, reason | ✅ |
| 7 | GET | /api/fund/expenses/{classroomId} | — | — | ✅ |
| 8 | POST | /api/fund/collections | X-User-Id | title, amount, classroomId, deadline | ✅ |
| 9 | GET | /api/fund/collections/{classroomId} | — | — | ✅ |
| 10 | GET | /api/fund/collections/{collectionId}/payments | — | — | ✅ |
| 11 | PUT | /api/fund/payments/{paymentId}/confirm | — | — | ✅ |
| 12 | GET | /api/fund/payments/my/{classroomId} | X-User-Id | — | ✅ |

---

## 6. Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| `Connection refused` | Server chưa chạy hoặc sai IP | Kiểm tra `mvn spring-boot:run`, check `ipconfig` |
| `400 - User không tồn tại` | Header `X-User-Id` sai hoặc thiếu | Thêm header đúng userId |
| `400 - Lớp học không tồn tại` | `classroomId` sai | Kiểm tra lại ID từ response tạo lớp |
| `400 - Tiêu đề ... không được để trống` | Body thiếu field bắt buộc | Thêm đủ field theo request |
| `400 - Bản ghi thanh toán không tồn tại` | `paymentId` sai | Gọi GET payments trước để lấy đúng ID |
