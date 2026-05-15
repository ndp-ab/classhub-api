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
- **Security:** ✅ `JwtAuthenticationFilter` validate Bearer token mọi `/api/**` trừ `/api/auth/**`. `AuthorizationService.requireMember/requireAdmin` check role theo lớp trong từng service. `SecurityUtil.currentUserId()` lấy userId từ SecurityContext (không còn `X-User-Id` header).
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

### Database (8 tables — ALL CREATED)
- [x] `users` (id, full_name, email, password, avatar_url, created_at)
- [x] `classrooms` (id, class_name, faculty, academic_year, invite_code, created_by, created_at)
- [x] `class_members` (id, user_id FK, classroom_id FK, role ENUM, joined_at) — unique(user_id, classroom_id)
- [x] `fund_collections` (id, title, amount, classroom_id FK, created_by FK, deadline, created_at)
- [x] `fund_payments` (id, user_id FK, collection_id FK, is_paid, confirmed_by_admin, paid_at)
- [x] `fund_expenses` (id, title, amount, classroom_id FK, created_by FK, reason, created_at)
- [x] `events` (id, title, description, location, event_time, classroom_id FK, created_by FK, created_at)
- [x] `event_participants` (id, event_id FK, user_id FK, checked_in, checked_in_at, registered_at) — unique(event_id, user_id)

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
- [x] `POST /api/events` — create event (Admin)
- [x] `GET /api/events/{classroomId}` — list events by classroom
- [x] `POST /api/events/{eventId}/volunteer` — register to attend
- [x] `DELETE /api/events/{eventId}/volunteer` — cancel registration
- [x] `GET /api/events/{eventId}/participants` — list participants (Admin)
- [x] `PUT /api/events/{eventId}/checkin/{userId}` — check-in (Admin)
- [x] `GET /api/events/my/{classroomId}` — my registered events

### Backend Files Created
```
com.classhub.classhubapi/
├── config/        SecurityConfig.java, JwtUtil.java
├── controller/    AuthController.java, ClassroomController.java,
│                  FundExpenseController.java, FundCollectionController.java,
│                  EventController.java
├── dto/           RegisterRequest, LoginRequest, AuthResponse,
│                  CreateClassroomRequest, JoinClassroomRequest, ClassroomResponse,
│                  CreateExpenseRequest, ExpenseResponse,
│                  CreateCollectionRequest, CollectionResponse,
│                  PaymentResponse, QrResponse, PaymentStatusResponse,
│                  CreateEventRequest, EventResponse, EventParticipantResponse
├── entity/        User, Classroom, ClassMember,
│                  FundCollection, FundPayment (+ paymentCode field), FundExpense,
│                  Event, EventParticipant
├── exception/     BadRequestException, GlobalExceptionHandler
├── repository/    UserRepository, ClassroomRepository, ClassMemberRepository,
│                  FundExpenseRepository, FundCollectionRepository, FundPaymentRepository,
│                  EventRepository, EventParticipantRepository
└── service/       AuthService, ClassroomService, FundExpenseService, FundCollectionService,
                   EventService
```

### Frontend Screens
- [x] `login_screen.dart` — email + password form
- [x] `signup_screen.dart` — fullName + email + password form
- [x] `home_screen.dart` — classroom list (tap card → detail)
- [x] `create_classroom_screen.dart` — form + shows invite code on success
- [x] `join_classroom_screen.dart` — invite code input
- [x] `classroom_detail_screen.dart` — TabBar 4 tab (Tổng quan / Khoản thu / Khoản chi / Sự kiện)
- [x] `fund/fund_tab.dart` — list khoản thu + "Khoản của bạn" cho Member
- [x] `fund/payment_qr_screen.dart` — QR + polling status 5s
- [x] `fund/collection_payments_screen.dart` — admin confirm payment
- [x] `fund/create_collection_screen.dart`
- [x] `fund/expenses_screen.dart` + `fund/create_expense_screen.dart`
- [x] `events/events_tab.dart` — Member đăng ký/huỷ, Admin tạo + xem participants
- [x] `events/create_event_screen.dart`
- [x] `events/event_participants_screen.dart` — Admin check-in

### Frontend Files Created
```
lib/
├── main.dart
├── models/        fund_collection.dart, payment.dart, expense.dart, event.dart
├── screens/
│   ├── login_screen.dart, signup_screen.dart, home_screen.dart
│   ├── create_classroom_screen.dart, join_classroom_screen.dart
│   ├── classroom_detail_screen.dart
│   ├── fund/      fund_tab, payment_qr_screen, collection_payments_screen,
│   │              create_collection_screen, expenses_screen, create_expense_screen
│   └── events/    events_tab, create_event_screen, event_participants_screen
├── providers/     auth_provider.dart
└── services/      auth_service.dart, classroom_service.dart,
                   fund_service.dart, event_service.dart
```

### Other
- [x] GitHub repos pushed (classhub-api + classhub-app)
- [x] Google Form survey created (not yet sent to class group)
- [x] Event analysis document (event-analysis.md)
- [ ] Figma wireframes (not started)
- [ ] Use Case Diagram, Sequence Diagram (not started)
- [ ] Report document (not started)

---

## 5. Blockers & Next Steps

> 2026-05-15: Đã vá xong B1–B8 (xem `documents/BACKEND_FIX_LOG.md`). BE compile sạch 52 file. JWT validated, authorization theo lớp đầy đủ, audit trail confirmedBy/checkedBy có. Bonus: PaymentResponse có amount+deadline, EventParticipantResponse có eventId.

### Đã làm trong commit B1–B8 (2026-05-15)
- ✅ `JwtAuthenticationFilter` + `SecurityUtil.currentUserId()` + `JwtAuthenticationEntryPoint`
- ✅ `AuthorizationService.requireMember/requireAdmin` gọi trong mọi service
- ✅ `FundPayment.confirmedBy` (@ManyToOne User) + idempotency check
- ✅ `EventParticipant.checkedBy` (@ManyToOne User)
- ✅ `@DecimalMin("0.01")` cho amount Collection/Expense
- ✅ `ClassroomService.joinClassroom` sinh payment bổ sung cho member join muộn
- ✅ `GlobalExceptionHandler` cover ForbiddenException, MethodArgumentNotValidException, MissingRequestHeader, HttpMessageNotReadable, generic Exception
- ✅ CORS toàn cục trong `SecurityConfig`, bỏ `@CrossOrigin` rải rác
- ✅ `PaymentResponse` thêm `amount`, `deadline`, `confirmedByName`
- ✅ `EventParticipantResponse` thêm `eventId`, `checkedByName`
- ✅ FE: bỏ `X-User-Id` ở 3 service (classroom/fund/event), chỉ giữ `Authorization: Bearer`
- ✅ FE: `fund_tab` hiển thị amount+deadline trong "Khoản của bạn"; `events_tab` match my-events qua eventId chuẩn (bỏ workaround title)

### Next Steps (tiếp theo)
1. API còn thiếu cho FE/demo:
   - `GET /api/classrooms/{id}/members` — unlock tab Thành viên (đang là placeholder)
   - `GET /api/classrooms/{id}/fund-statistics` — số liệu cho slide demo
   - `POST /api/events/{eventId}/assign` — admin chỉ định participant
2. Mở rộng `EventParticipant`: `type` (VOLUNTEER/ASSIGNED), `attendanceStatus` (PENDING/PRESENT/ABSENT)
3. `Event` thêm `endTime` + validate `eventTime >= now`
4. Postman collection + Test case TC01–TC20 (checklist mục 11)
5. Figma wireframes (backfill)
6. Use Case Diagram + Sequence Diagram
7. Report + slides

### Known Issues (còn lại sau B1–B8)
- `Classroom.createdBy` dùng `Long` thay vì `@ManyToOne User` (cosmetic, không ảnh hưởng demo)
- `FundPayment.isPaid` và `confirmedByAdmin` redundant — luôn cùng giá trị (nên dùng enum status)
- `FundCollection` thiếu `description` (FE đã bỏ)
- IP thay đổi khi đổi WiFi — cần update `baseUrl` trong 4 service file FE
- VietQR `vietqr.account-no` trong `application.properties` cần thay TK thật trước demo
- `Event` thiếu `endTime`
- `JwtAuthenticationFilter` deprecation warning (WebAuthenticationDetailsSource) — vô hại

### Instructor Requirements (thầy Dũng/Hiếu)
- Must justify every design decision with evidence (survey, analysis)
- OOAD methodology required: Use Case, ERD, Sequence Diagrams
- Figma wireframes before coding (already violated — need to backfill)
- Needs analysis survey (Google Form created, not yet distributed)
- Compare with existing solutions (Zalo + Excel vs ClassHub)
- Demo video for progress report (script already written)
- All actors/features must be logically connected, not isolated