# BACKEND_FIX_LOG — 2026-05-15

Vá toàn bộ 8 blocker B1–B8 từ `BACKEND_AUDIT.md`. BE compile sạch 52 file, FE đã đồng bộ.

---

## B1 — JWT validation thật

**Mục đích:** Không còn tin `X-User-Id` do client tự khai. Mọi `/api/**` (trừ `/api/auth/**`) yêu cầu `Authorization: Bearer <token>` hợp lệ.

**File:**
- ✨ `config/JwtAuthenticationFilter.java` — `OncePerRequestFilter`, đọc Bearer, gọi `JwtUtil.validateToken` + `getUserIdFromToken`, set principal là `Long userId` vào `SecurityContextHolder`.
- ✨ `config/JwtAuthenticationEntryPoint.java` — trả JSON 401 thay HTML login mặc định.
- ✨ `config/SecurityUtil.java` — `currentUserId()` lấy userId từ SecurityContext, ném `ForbiddenException("Chưa đăng nhập")` nếu rỗng.
- ✏️ `config/JwtUtil.java` — thêm `getUserIdFromToken(String)`.
- ✏️ `config/SecurityConfig.java` — bỏ permitAll, register filter trước `UsernamePasswordAuthenticationFilter`, session STATELESS.
- ✏️ Tất cả controller — bỏ `@RequestHeader("X-User-Id")`, dùng `SecurityUtil.currentUserId()`.

---

## B2 — Authorization theo lớp

**Mục đích:** Member không gọi được API admin. Admin lớp A không đụng được dữ liệu lớp B. User ngoài lớp không xem được dữ liệu lớp.

**File:**
- ✨ `service/AuthorizationService.java` — `requireMember(userId, classroomId)` + `requireAdmin(userId, classroomId)`. Vi phạm → `ForbiddenException` → handler trả 403.
- ✏️ `repository/ClassMemberRepository.java` — thêm `findByUserIdAndClassroomId` để check role.
- ✏️ `FundCollectionService` — gọi `requireAdmin` ở create/confirm/getPaymentsByCollection; `requireMember` ở getCollections/getMyPayments. `generateQr`/`getPaymentStatus` check `payment.user.id == currentUserId` (owner-only).
- ✏️ `FundExpenseService` — `requireAdmin` ở create, `requireMember` ở list.
- ✏️ `EventService` — `requireAdmin` ở create/getParticipants/checkIn; `requireMember` ở getEvents/volunteer/getMyEvents.

---

## B3 — `FundPayment.confirmedBy` + idempotency

**Mục đích:** Truy vết được "ai đã xác nhận khoản này". Chặn double-confirm.

**File:**
- ✏️ `entity/FundPayment.java` — thêm `@ManyToOne(LAZY) User confirmedBy` (cột DB `confirmed_by`).
- ✏️ `service/FundCollectionService.confirmPayment` — đổi signature thành `(paymentId, adminUserId)`. Check idempotency: `if (payment.isConfirmedByAdmin()) throw new BadRequestException("Khoản thu này đã được xác nhận")`. Set `confirmedBy = admin`.
- ✏️ `dto/PaymentResponse.java` — thêm `amount`, `deadline` (bonus cho FE), `confirmedByName`.
- ✏️ `controller/FundCollectionController` — bỏ header X-User-Id, dùng `SecurityUtil.currentUserId()`.

---

## B4 — `EventParticipant.checkedBy`

**Mục đích:** Tương tự B3 cho check-in sự kiện.

**File:**
- ✏️ `entity/EventParticipant.java` — thêm `@ManyToOne(LAZY) User checkedBy` (cột DB `checked_by`).
- ✏️ `service/EventService.checkIn` — đổi signature thành `(eventId, targetUserId, adminUserId)`, gọi `requireAdmin`, set `checkedBy = admin`.
- ✏️ `dto/EventParticipantResponse.java` — thêm `eventId` (giải workaround FE) + `checkedByName`.

---

## B5 — Validation `amount > 0`

**File:**
- ✏️ `dto/CreateCollectionRequest.java` — thêm `@DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")` cho `amount`.
- ✏️ `dto/CreateExpenseRequest.java` — tương tự.

---

## B6 — Member join trễ → sinh payment bổ sung

**Mục đích:** Sinh viên tham gia lớp SAU khi đã có khoản thu vẫn thấy nợ.

**File:**
- ✏️ `service/ClassroomService.java` — inject `FundCollectionRepository` + `FundPaymentRepository`. Trong `joinClassroom`, sau khi `save(member)`, query tất cả `FundCollection` của lớp, mỗi cái tạo `FundPayment(user, collection, isPaid=false)` nếu chưa có.
- ✏️ Đổi `RuntimeException` → `BadRequestException` cho thông báo lỗi đẹp hơn (nhân tiện sửa).

---

## B7 — `GlobalExceptionHandler` mở rộng

**File:**
- ✨ `exception/ForbiddenException.java` — runtime exception cho 403.
- ✏️ `exception/GlobalExceptionHandler.java` — handler cho:
  - `BadRequestException` → 400 (đã có)
  - `ForbiddenException` → 403
  - `MethodArgumentNotValidException` → 400 với map field errors
  - `MissingRequestHeaderException` → 400
  - `HttpMessageNotReadableException` → 400 (body JSON lỗi)
  - Generic `Exception` → 500 với message gọn (log stacktrace nội bộ)

Tất cả trả JSON `{status, message}` thay vì HTML stacktrace.

---

## B8 — CORS toàn cục

**File:**
- ✏️ `config/SecurityConfig.java` — thêm bean `CorsConfigurationSource` allow tất cả origin pattern, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, expose Authorization header. Gắn vào filter chain.
- ✏️ Bỏ `@CrossOrigin(origins="*")` ở `AuthController`, `ClassroomController`, `FundExpenseController`.

---

## Đồng bộ Flutter (classhub_app)

Sau B1, BE không nhận `X-User-Id` nữa → FE phải gửi `Authorization: Bearer`.

**File:**
- ✏️ `lib/services/classroom_service.dart` — viết lại helper `_headers()` lấy token từ `SharedPreferences`, gửi Bearer.
- ✏️ `lib/services/fund_service.dart` — bỏ `X-User-Id`, chỉ Bearer.
- ✏️ `lib/services/event_service.dart` — bỏ `X-User-Id`, chỉ Bearer.
- ✏️ `lib/models/payment.dart` — parse thêm `amount`, `deadline`, `confirmedByName`.
- ✏️ `lib/models/event.dart` — `EventParticipant` parse thêm `eventId`, `checkedByName`.
- ✏️ `lib/screens/fund/fund_tab.dart` — hiển thị `amount` + `deadline` trong "Khoản của bạn"; hiển thị tên người xác nhận.
- ✏️ `lib/screens/events/events_tab.dart` — bỏ workaround match qua title, match qua `eventId`.

---

## Verify

```
> ./mvnw clean -DskipTests compile
[INFO] Compiling 52 source files
[INFO] BUILD SUCCESS
```

Một warning deprecated `WebAuthenticationDetailsSource` ở `JwtAuthenticationFilter` — vô hại, vẫn hoạt động.

---

## Trạng thái sau khi vá: trả lời được 5 câu hội đồng

| Câu hỏi hội đồng | Trả lời sau B1–B8 |
|---|---|
| "Thầy không có token, có vào được API không?" | Không. JwtAuthenticationEntryPoint trả 401 JSON. |
| "Admin lớp A có confirm được payment lớp B không?" | Không. `AuthorizationService.requireAdmin` check role theo `payment.fundCollection.classroom.id`. |
| "Ai là người xác nhận khoản này?" | `payment.confirmedBy` (entity) + `confirmedByName` (response). |
| "SV tham gia lớp sau khi đã có khoản thu thì sao?" | `ClassroomService.joinClassroom` tự sinh payment bổ sung. |
| "Em có chặn gọi API admin của member không?" | Có. `requireAdmin` ném `ForbiddenException` → 403. |

---

## Còn lại (đã chuyển sang Next Steps trong `ai-context-md.md`)

- API members, fund-statistics, event-statistics, assign participant
- Mở rộng `EventParticipant` thêm `type`/`attendanceStatus`
- `Event.endTime` + validation start<end
- Postman collection + Test case TC01–TC20
- `Classroom.createdBy: Long` → `@ManyToOne User`
- Gộp `isPaid`/`confirmedByAdmin` thành enum status
