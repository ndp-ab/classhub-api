# AI_CONTEXT.md — ClassHub Project Memory

## 1. Project Overview

ClassHub là ứng dụng di động quản lý hoạt động lớp học đại học (đồ án tốt nghiệp), gồm 3 phân hệ: Hồ sơ & Phân quyền, Quỹ lớp (thu/chi + QR VietQR), Sự kiện & Điểm danh. Solo project bởi Nguyễn Duy Phong (64KTPM3, MSV 2251172450), GVHD: Th.S. Tạ Chí Hiếu.

---

## 2. Tech Stack & Architecture

### Backend
- **Framework:** Spring Boot (Maven, Java 17)
- **Dependencies:** Spring Web, Spring Data JPA, MySQL Driver, Spring Security, Lombok, Validation, JWT (jjwt 0.12.6)
- **DB:** MySQL 8+ (`classhub_db`, user: `classhub_user`)
- **Architecture:** Layered — `entity → repository → service → controller → dto`
- **Error handling:** `GlobalExceptionHandler` + custom `BadRequestException` → returns 400 JSON
- **Auth:** JWT token (24h expiry, HMAC-SHA256), BCrypt password hashing
- **Security:** `/api/auth/**` và `/api/classrooms/**` và `/api/fund/**` = permitAll (tạm thời, chưa có JWT filter)
- **Repo:** `github.com/agio7/classhub-api`

### Frontend
- **Framework:** Flutter (Dart)
- **Pattern:** MVVM + Provider
- **Structure:** `screens/` (View) → `providers/` (ViewModel) → `services/` (Model)
- **State:** Provider (ChangeNotifier), SharedPreferences (token persistence)
- **Base URL:** `http://192.168.1.5:8080/api` (thay đổi theo IP WiFi, check `ipconfig`)
- **Repo:** `github.com/agio7/classhub-app`

### Tools
- **IDE:** IntelliJ (backend), Android Studio (frontend)
- **Test API:** Postman
- **Design:** Figma (chưa bắt đầu)
- **Survey:** Google Form (đã tạo, chưa gửi)
- **VCS:** Git + GitHub (2 repo riêng)

---

## 3. Core Conventions

### Backend (Java/Spring Boot)
- Package names: lowercase singular (`entity`, `controller`, `dto`)
- Class names: PascalCase (`AuthController`, `FundExpenseService`)
- DB table names: snake_case plural (`fund_collections`, `class_members`)
- DB column names: snake_case (`full_name`, `invite_code`)
- Java fields: camelCase (`isPaid`, `confirmedByAdmin`) — JPA auto-maps to snake_case
- API URLs: lowercase (`/api/auth/register`, `/api/fund/expenses`)
- FK relationships: use `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn`, NOT raw `Long foreignId`
  - Exception: `Classroom.createdBy` still uses `Long` (legacy, should refactor)
- Money fields: `BigDecimal`, NEVER `double` (floating point precision)
- Timestamps: `@CreationTimestamp` + `LocalDateTime` + `updatable = false`
- Validation: `@NotBlank` for strings, `@NotNull` for numbers, `@Email` for emails
- Errors: throw `BadRequestException("message")`, NOT `RuntimeException`
- DTO pattern: `XxxRequest` (input), `XxxResponse` (output) — never expose entity directly
- Invite code: 6-char uppercase from `UUID.randomUUID().substring(0,6).toUpperCase()`

### Frontend (Flutter/Dart)
- File names: snake_case (`auth_service.dart`, `login_screen.dart`)
- Class names: PascalCase (`AuthProvider`, `LoginScreen`)
- Screen → Provider → Service (never call Service directly from Screen)
  - Exception: `ClassroomService` called directly from screens (no ClassroomProvider yet)
- Auth state: `AuthProvider` holds token, userId, fullName, email
- Navigation: `Navigator.pushReplacement` (login→home), `Navigator.pushAndRemoveUntil` (logout)
- User ID: passed via `X-User-Id` header (temporary, should use JWT filter later)

---

## 4. Current Status

### Database (6 tables — ALL CREATED)
- [x] `users` (id, full_name, email, password, avatar_url, created_at)
- [x] `classrooms` (id, class_name, faculty, academic_year, invite_code, created_by, created_at)
- [x] `class_members` (id, user_id FK, classroom_id FK, role ENUM, joined_at) — unique(user_id, classroom_id)
- [x] `fund_collections` (id, title, amount, classroom_id FK, created_by FK, deadline, created_at)
- [x] `fund_payments` (id, user_id FK, collection_id FK, is_paid, confirmed_by_admin, paid_at)
- [x] `fund_expenses` (id, title, amount, classroom_id FK, created_by FK, reason, created_at)

### Backend APIs
- [x] `POST /api/auth/register` — register + return JWT
- [x] `POST /api/auth/login` — login + return JWT
- [x] `POST /api/classrooms/create` — create class + auto ADMIN role
- [x] `POST /api/classrooms/join` — join by invite code + auto MEMBER role
- [x] `GET /api/classrooms/my` — list my classrooms with roles
- [x] `POST /api/fund/expenses` — create expense
- [x] `GET /api/fund/expenses/{classroomId}` — list expenses by classroom
- [x] `POST /api/fund/collections` — create collection + auto-create payments for all members
- [x] `GET /api/fund/collections/{classroomId}` — list collections
- [x] `GET /api/fund/collections/{collectionId}/payments` — list payment status per member
- [x] `PUT /api/fund/payments/{paymentId}/confirm` — admin confirm payment
- [x] `GET /api/fund/payments/my/{classroomId}` — my debts
- [x] `GET /api/fund/payments/{paymentId}/qr` — generate VietQR URL + paymentCode
- [x] `GET /api/fund/payments/{paymentId}/status` — polling payment status (PENDING/CONFIRMED)
- [ ] Event APIs (not started) ⬅️ **NEXT**

### Backend Files Created
```
com.classhub.classhubapi/
├── config/        SecurityConfig.java, JwtUtil.java
├── controller/    AuthController.java, ClassroomController.java,
│                  FundExpenseController.java, FundCollectionController.java
├── dto/           RegisterRequest, LoginRequest, AuthResponse,
│                  CreateClassroomRequest, JoinClassroomRequest, ClassroomResponse,
│                  CreateExpenseRequest, ExpenseResponse,
│                  CreateCollectionRequest, CollectionResponse,
│                  PaymentResponse, QrResponse, PaymentStatusResponse
├── entity/        User, Classroom, ClassMember,
│                  FundCollection, FundPayment (+ paymentCode field), FundExpense
├── exception/     BadRequestException, GlobalExceptionHandler
├── repository/    UserRepository, ClassroomRepository, ClassMemberRepository,
│                  FundExpenseRepository, FundCollectionRepository, FundPaymentRepository
└── service/       AuthService, ClassroomService, FundExpenseService, FundCollectionService
```

### Frontend Screens
- [x] `login_screen.dart` — email + password form
- [x] `signup_screen.dart` — fullName + email + password form
- [x] `home_screen.dart` — classroom list + create/join buttons
- [x] `create_classroom_screen.dart` — form + shows invite code on success
- [x] `join_classroom_screen.dart` — invite code input
- [ ] Classroom detail screen (Bottom Navigation with tabs)
- [ ] Fund tab (collections + expenses + summary)
- [ ] Event tab

### Frontend Files Created
```
lib/
├── main.dart
├── screens/       login, signup, home, create_classroom, join_classroom
├── providers/     auth_provider.dart
└── services/      auth_service.dart, classroom_service.dart
```

### Other
- [x] GitHub repos pushed (classhub-api + classhub-app)
- [x] Google Form survey created (not yet sent to class group)
- [ ] Figma wireframes (not started)
- [ ] Use Case Diagram, Sequence Diagram (not started)
- [ ] Report document (not started)

---

## 5. Blockers & Next Steps

### Immediate Next Task
**Flutter: màn hình Quỹ lớp** — hiển thị danh sách khoản thu và tích hợp QR payment:
1. Màn hình chi tiết lớp (Bottom Navigation: Quỹ / Sự kiện)
2. Tab Quỹ: list khoản thu + khoản chi
3. Màn hình đóng quỹ: hiển thị QR từ `/qr` endpoint
4. Polling `/status` mỗi 5s → chuyển màn khi CONFIRMED

### Known Issues
- `X-User-Id` header is insecure — need JWT filter to extract userId from token
- `Classroom.createdBy` uses `Long` instead of `@ManyToOne` (inconsistent with newer entities)
- No CORS config (may need when Flutter web testing)
- IP changes on WiFi reconnect — need to update `baseUrl` in both service files
- VietQR `vietqr.account-no` in `application.properties` cần thay bằng số TK thật trước demo

### Upcoming Tasks (Priority Order)
1. Flutter: classroom detail screen (Bottom Navigation)
2. Flutter: fund tab — danh sách khoản thu + khoản chi
3. Flutter: QR payment screen (polling status)
4. Event APIs (create event, volunteer, check-in)
5. Flutter: event tab
6. JWT filter (replace X-User-Id header)
7. Figma wireframes (backfill)
8. Use Case Diagram + Sequence Diagram
9. Report + slides

### Instructor Requirements (thầy Dũng/Hiếu)
- Must justify every design decision with evidence (survey, analysis)
- OOAD methodology required: Use Case, ERD, Sequence Diagrams
- Figma wireframes before coding (already violated — need to backfill)
- Needs analysis survey (Google Form created, not yet distributed)
- Compare with existing solutions (Zalo + Excel vs ClassHub)
- Demo video for progress report (script already written)
- All actors/features must be logically connected, not isolated