# ClassHub — Tài liệu tổng quan dự án

## Thông tin chung
- **Đề tài:** Phát triển Ứng dụng Di động Hỗ trợ Quản lý Hoạt động Lớp học
- **Sinh viên:** Nguyễn Duy Phong — 64KTPM3 — MSV: 2251172450
- **GVHD:** Th.S. Tạ Chí Hiếu
- **Công nghệ:** Spring Boot (Backend) + Flutter (Frontend) + MySQL (CSDL)
- **GitHub:** [classhub-api](https://github.com/agio7/classhub-api) | [classhub-app](https://github.com/agio7/classhub-app)

---

## 1. Bài toán & Động lực

### Thực trạng
Quản lý sinh viên tại các lớp đại học hiện nay chủ yếu dựa vào:
- Zalo / Messenger: thông báo bị trôi, không thể tìm lại
- Google Sheets / Excel: dữ liệu rải rác, khó đối soát, dễ sửa nhầm
- Ghi chép thủ công: không minh bạch, dễ tranh cãi

### Các vấn đề cụ thể
1. **Thông báo bị trôi:** Tin nhắn quan trọng bị chìm trong group chat hàng trăm tin nhắn mỗi ngày
2. **Quỹ lớp không minh bạch:** Ban cán sự ghi sổ bằng Excel, sinh viên không kiểm chứng được, dễ xảy ra tranh cãi
3. **Điểm danh sự kiện thủ công:** Ban cán sự gọi tên từng người hoặc truyền giấy ký, tốn thời gian, dễ sai sót
4. **Dữ liệu phân tán:** Mỗi khóa, mỗi lớp dùng một cách khác nhau, không có hệ thống tập trung

### Giải pháp ClassHub
Xây dựng ứng dụng di động tập trung theo mô hình Client-Server, số hóa 3 nghiệp vụ cốt lõi:
- Quản lý hồ sơ & phân quyền
- Quản lý tài chính quỹ lớp (có tích hợp QR VietQR)
- Thống kê sự kiện & điểm danh

### So sánh với giải pháp hiện tại
| Tiêu chí | Zalo + Excel | ClassHub |
|----------|-------------|----------|
| Thông báo | Bị trôi trong group | Tập trung trên app |
| Quỹ lớp | Ghi sổ thủ công, dễ tranh cãi | Minh bạch, có lịch sử, QR chuyển khoản |
| Điểm danh | Gọi tên / truyền giấy | Tự động qua app |
| Phân quyền | Không có | ADMIN / MEMBER theo ngữ cảnh lớp |
| Đối soát | Phải so sánh thủ công | Nội dung chuyển khoản tự chứa mã SV |

---

## 2. Kiến trúc hệ thống

### Tổng thể: Client-Server
```
Flutter App (Android) ←→ REST API ←→ Spring Boot ←→ MySQL
```

### Backend: Kiến trúc phân lớp (Spring Boot)
```
Controller (nhận request)
    ↓
DTO (lọc dữ liệu vào/ra)
    ↓
Service (xử lý logic nghiệp vụ)
    ↓
Repository (truy vấn database)
    ↓
Entity (mapping bảng MySQL)
```
- **Bảo mật:** JWT (jjwt 0.12.6) + BCrypt password hashing
- **Xử lý lỗi:** GlobalExceptionHandler + custom BadRequestException (trả JSON chuẩn)

### Frontend: MVVM + Provider (Flutter)
```
Screens (View — giao diện)
    ↓
Providers (ViewModel — logic + state)
    ↓
Services (Model — gọi API)
```
- **State management:** Provider (ChangeNotifier)
- **Lưu trữ local:** SharedPreferences (token, user info)

---

## 3. Thiết kế cơ sở dữ liệu

### 3.1 Phân hệ Hồ sơ & Phân quyền (3 bảng)

**users** — Tài khoản người dùng
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| full_name | varchar | Họ tên |
| email | varchar UK | Email đăng nhập (unique) |
| password | varchar | Mật khẩu đã mã hóa BCrypt |
| avatar_url | varchar | Ảnh đại diện (nullable) |
| created_at | datetime | Ngày tạo tài khoản |

**classrooms** — Lớp học
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| class_name | varchar | Tên lớp (VD: 64KTPM3) |
| faculty | varchar | Khoa |
| academic_year | varchar | Khóa (VD: K64) |
| invite_code | varchar UK | Mã 6 ký tự để join lớp |
| created_by | bigint FK → users | Người tạo lớp |
| created_at | datetime | Ngày tạo |

**class_members** — Quan hệ user-classroom (bảng trung gian)
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| user_id | bigint FK → users | Sinh viên nào |
| classroom_id | bigint FK → classrooms | Lớp nào |
| role | enum (ADMIN/MEMBER) | Vai trò trong lớp |
| joined_at | datetime | Ngày tham gia |

**Thiết kế đặc biệt:** Phân quyền theo ngữ cảnh (context-based role). Một user có thể là ADMIN ở lớp A nhưng MEMBER ở lớp B. Khác với phân quyền toàn cục (gắn cứng role vào tài khoản).

### 3.2 Phân hệ Quỹ lớp (3 bảng)

**fund_collections** — Khoản thu
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| title | varchar | Tên khoản thu (VD: "Quỹ tháng 4") |
| amount | decimal | Số tiền mỗi người phải đóng |
| classroom_id | bigint FK → classrooms | Thuộc lớp nào |
| created_by | bigint FK → users | Admin nào tạo |
| deadline | date | Hạn đóng |
| created_at | datetime | Ngày tạo |

**fund_payments** — Trạng thái đóng tiền từng người
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| user_id | bigint FK → users | Sinh viên nào |
| collection_id | bigint FK → fund_collections | Đóng cho khoản nào |
| is_paid | boolean | Đã đóng chưa (mặc định false) |
| confirmed_by_admin | boolean | Admin xác nhận chưa (mặc định false) |
| paid_at | datetime | Thời gian đóng (nullable) |

**fund_expenses** — Khoản chi
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| title | varchar | Tên khoản chi (VD: "Mua hoa 20/11") |
| amount | decimal | Số tiền chi |
| classroom_id | bigint FK → classrooms | Chi từ quỹ lớp nào |
| created_by | bigint FK → users | Ai tạo khoản chi |
| reason | varchar | Lý do chi (nullable) |
| created_at | datetime | Ngày tạo |

**Tính tổng quỹ:** Không lưu sẵn mà tính runtime từ dữ liệu gốc (single source of truth):
- Tổng thu = SUM từ fund_payments WHERE confirmed_by_admin = true
- Tổng chi = SUM(amount) từ fund_expenses
- Số dư = Tổng thu - Tổng chi

### 3.3 Mối quan hệ tổng thể
```
USERS ──1:N──→ CLASSROOMS (creates)
USERS ──1:N──→ CLASS_MEMBERS (joins)
CLASSROOMS ──1:N──→ CLASS_MEMBERS (has)
CLASSROOMS ──1:N──→ FUND_COLLECTIONS (has)
CLASSROOMS ──1:N──→ FUND_EXPENSES (has)
FUND_COLLECTIONS ──1:N──→ FUND_PAYMENTS (has)
USERS ──1:N──→ FUND_PAYMENTS (pays)
USERS ──1:N──→ FUND_EXPENSES (creates)
```

---

## 4. Danh sách API

### 4.1 Auth (đã hoàn thiện)
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/auth/register | Đăng ký tài khoản |
| POST | /api/auth/login | Đăng nhập, trả JWT token |

### 4.2 Classroom (đã hoàn thiện)
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/classrooms/create | Tạo lớp + sinh invite code |
| POST | /api/classrooms/join | Join lớp bằng invite code |
| GET | /api/classrooms/my | Xem danh sách lớp của mình |

### 4.3 Quỹ lớp — Khoản chi (đã hoàn thiện)
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/fund/expenses | Tạo khoản chi |
| GET | /api/fund/expenses/{classroomId} | Xem danh sách khoản chi |

### 4.4 Quỹ lớp — Khoản thu (đã hoàn thiện ✅)
- [x] `POST /api/fund/expenses` — create expense
- [x] `GET /api/fund/expenses/{classroomId}` — list expenses by classroom
- [x] `POST /api/fund/collections` — create collection + auto-create payments for all members
- [x] `GET /api/fund/collections/{classroomId}` — list collections
- [x] `GET /api/fund/collections/{collectionId}/payments` — list payment status per member
- [x] `PUT /api/fund/payments/{paymentId}/confirm` — admin confirm payment
- [x] `GET /api/fund/payments/my/{classroomId}` — my debts
- [ ] Event APIs (not started) ⬅️ **NEXT**

### 4.5 Sự kiện (chưa triển khai)
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/events | Tạo sự kiện |
| GET | /api/events/{classroomId} | Xem danh sách sự kiện |
| POST | /api/events/{id}/volunteer | Xung phong tham gia |
| PUT | /api/events/{id}/checkin/{userId} | Check-in |

---

## 5. Cấu trúc code

### Backend (Spring Boot)
```
com.classhub.classhubapi/
├── config/          SecurityConfig, JwtUtil
├── controller/      AuthController.java, ClassroomController.java,
│                    FundExpenseController.java, FundCollectionController.java
├── dto/             RegisterRequest, LoginRequest, AuthResponse,
│                    CreateClassroomRequest, JoinClassroomRequest, ClassroomResponse,
│                    CreateExpenseRequest, ExpenseResponse,
│                    CreateCollectionRequest, CollectionResponse,
│                    PaymentResponse, QrResponse, PaymentStatusResponse
├── entity/          User, Classroom, ClassMember,
│                    FundCollection, FundPayment (+ paymentCode), FundExpense
├── exception/       BadRequestException, GlobalExceptionHandler
├── repository/      UserRepository, ClassroomRepository, ClassMemberRepository,
│                    FundExpenseRepository, FundCollectionRepository, FundPaymentRepository
└── service/         AuthService, ClassroomService, FundExpenseService, FundCollectionService
```

### Frontend (Flutter)
```
lib/
├── main.dart                          Entry point + AuthWrapper
├── screens/
│   ├── login_screen.dart              Màn hình đăng nhập
│   ├── signup_screen.dart             Màn hình đăng ký
│   ├── home_screen.dart               Danh sách lớp + nút tạo/join
│   ├── create_classroom_screen.dart   Tạo lớp mới
│   └── join_classroom_screen.dart     Join lớp bằng invite code
├── providers/
│   └── auth_provider.dart             Quản lý state đăng nhập
└── services/
    ├── auth_service.dart              Gọi API auth
    └── classroom_service.dart         Gọi API classroom
```

---

## 6. Tiến độ hiện tại

### Đã hoàn thiện
- [x] Database 6 bảng (MySQL)
- [x] API Auth: đăng ký + đăng nhập + JWT
- [x] API Classroom: tạo lớp + join lớp + xem danh sách
- [x] API Quỹ lớp: khoản chi (tạo + xem danh sách)
- [x] API Quỹ lớp: khoản thu (tạo + xem + xem chi tiết payment + xác nhận + nợ cá nhân)
- [x] API Quỹ lớp: QR VietQR (generate URL + polling status) — Backend hoàn thiện
- [x] Xử lý lỗi tập trung (GlobalExceptionHandler)
- [x] Flutter: đăng nhập, đăng ký, trang chủ, tạo lớp, join lớp
- [x] GitHub: 2 repo đã push
- [x] Google Form khảo sát (đã tạo)

### Upcoming Tasks (Priority Order)
1. Flutter: classroom detail screen (Bottom Navigation)
2. Flutter: fund tab — danh sách khoản thu + khoản chi
3. Flutter: QR payment screen (polling `/status` mỗi 5s)
4. Event APIs (create event, volunteer, check-in)
5. Flutter: event tab
6. JWT filter (replace X-User-Id header)
7. Figma wireframes (backfill)
8. Use Case Diagram + Sequence Diagram
9. Report + slides

---

## 7. Quyết định thiết kế & Lý do

| Quyết định | Lý do |
|-----------|-------|
| Phân quyền theo ngữ cảnh lớp (không phải toàn cục) | 1 người có thể ADMIN lớp A, MEMBER lớp B |
| Invite code 6 ký tự UUID | Đơn giản, dễ chia sẻ qua Zalo, không cần hệ thống phức tạp |
| BigDecimal cho số tiền | double bị lỗi làm tròn khi tính tiền |
| Không lưu tổng quỹ vào DB | Single source of truth — tính từ dữ liệu gốc luôn chính xác |
| QR VietQR (Mức 2) | Vừa sức đồ án, có điểm nhấn kỹ thuật, không cần merchant account |
| MVVM + Provider cho Flutter | Đơn giản, Flutter team khuyên dùng, đủ cho quy mô đồ án |
| @ManyToOne thay vì Long FK | Database tự kiểm tra ràng buộc, query tiện hơn, code rõ ràng hơn |
| GlobalExceptionHandler | Trả lỗi JSON chuẩn, Flutter đọc được message cụ thể |