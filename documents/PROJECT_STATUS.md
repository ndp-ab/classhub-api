# PROJECT_STATUS — ClassHub

> File này tổng hợp **đã làm gì** và **còn tồn đọng** ở cả BE + FE.
> Đọc file này trước khi quay lại làm tiếp.
> Cập nhật lần cuối: 2026-05-16 (sau khi áp dụng GP1: Member báo đã CK).

---

## ✅ Đã làm

### Backend (`classhub-api`)

**Phân hệ chính** (3/3 đề cương):
- Auth: register/login + JWT generation + BCrypt hash
- Classroom: create / join (invite code) / list-my
- Fund: collections (auto-sinh payment), payments (confirm), QR VietQR, status polling, expenses
- Event: create / list / volunteer / cancel-volunteer / participants / check-in / my-events

**Đã thêm GP1 (2026-05-16) — Member tự báo đã chuyển khoản:**
- Endpoint mới `POST /api/fund/payments/{paymentId}/mark-paid` (owner-only).
- `FundPayment.markedPaidAt` lưu thời điểm Member báo.
- 3 trạng thái: `UNPAID` → `PENDING_VERIFICATION` → `CONFIRMED`.
- 2 boolean cũ (`isPaid`, `confirmedByAdmin`) giờ có ý nghĩa khác nhau (fix luôn redundancy).
- FE: nút "Tôi đã chuyển khoản" trong QR screen + 3-state UI trong Fund tab + Admin priority queue.

**Đã vá B1–B8 (2026-05-15):**
| Block | Đã làm |
|---|---|
| B1 | `JwtAuthenticationFilter` validate Bearer mọi `/api/**` trừ `/api/auth`. `SecurityUtil.currentUserId()`. `JwtAuthenticationEntryPoint` trả JSON 401. |
| B2 | `AuthorizationService.requireMember/requireAdmin` gọi trong mọi service Fund/Event/Expense. |
| B3 | `FundPayment.confirmedBy` + idempotency check ở `confirmPayment`. `PaymentResponse.confirmedByName + amount + deadline`. |
| B4 | `EventParticipant.checkedBy`. `EventParticipantResponse.checkedByName + eventId`. |
| B5 | `@DecimalMin("0.01")` cho `amount` ở `CreateCollectionRequest` + `CreateExpenseRequest`. |
| B6 | `ClassroomService.joinClassroom` tự sinh payment bổ sung cho member join muộn. |
| B7 | `GlobalExceptionHandler` cover thêm ForbiddenException / validation / missing header / bad JSON / generic Exception. |
| B8 | CORS toàn cục trong `SecurityConfig`. Bỏ `@CrossOrigin` rải rác ở controller. |

**Verify:** `mvnw.cmd clean test` → BUILD SUCCESS (52 source files, 1 test pass).

### Frontend (`classhub_app`)

**Screens đã có:**
- Login, signup, home (list lớp)
- Create classroom, join classroom
- Classroom detail (TabBar 4 tab: Tổng quan / Khoản thu / Khoản chi / Sự kiện)
- Fund tab: list đợt thu + "Khoản của bạn" cho Member
- Payment QR screen (polling 5s)
- Collection payments (admin xác nhận)
- Create collection
- Expenses list + Create expense
- Events tab (đăng ký/huỷ cho Member, tạo + xem participants cho Admin)
- Create event (date+time picker)
- Event participants (admin check-in)

**Services/Models:**
- `auth_service`, `classroom_service`, `fund_service`, `event_service` — đều dùng `Authorization: Bearer` (bỏ X-User-Id sau B1).
- Models: `fund_collection`, `payment` (parse `amount`, `deadline`, `confirmedByName`), `expense`, `event` (`ClassEvent` + `EventParticipant` với `eventId`, `checkedByName`).

---

## ❌ Còn tồn đọng (xếp theo ưu tiên)

### 🔴 PHẢI làm trước demo

| # | Việc | Effort | Lý do |
|---|---|---|---|
| 1 | `GET /api/classrooms/{id}/members` | ~30 phút | FE tab Thành viên đang placeholder |
| 2 | `GET /api/classrooms/{id}/fund-statistics` (tổng thu/chi/dư/số người nợ) | ~1 tiếng | Slide demo cần số liệu |
| 3 | Smoke test BE 1 luồng E2E qua Postman | ~15 phút | Mình compile sạch nhưng chưa chạy thật — cần verify Hibernate auto-add 2 cột `confirmed_by`, `checked_by` |

### 🟡 NÊN làm để khỏi bị bắt lẻ

| # | Việc | Effort |
|---|---|---|
| 4 | `@Future` cho `Event.eventTime` + `FundCollection.deadline >= today` | 5 phút |
| 5 | `Event.endTime` + validate `start < end` | 15 phút |
| 6 | `POST /api/events/{id}/assign` (admin chỉ định participant) | 30 phút |
| 7 | `PUT/DELETE` cho collection / event / expense | 1 tiếng |
| 8 | Test case TC01–TC20 với MockMvc | 3-4 tiếng |
| 9 | Postman collection export `.json` | 30 phút |

### 🟢 Đưa vào "Hướng phát triển" (slide)

| # | Việc | Vì sao không làm |
|---|---|---|
| 10 | Role `OWNER` (3 cấp) | ADMIN/MEMBER đủ demo |
| 11 | `EventParticipant.type` (VOLUNTEER/ASSIGNED) + `attendanceStatus` (PRESENT/ABSENT) | Boolean `checkedIn` đủ demo |
| 12 | Notification + push (FCM) | Checklist nói "điểm cộng, không bắt buộc" |
| 13 | `/api/auth/me` | Info đã có ở login response |
| 14 | Promote/demote/kick member API | Không cần cho demo |
| 15 | Refresh token, import Excel, PDF export | Quá nhiều việc |

### ⚪ Cosmetic — KHÔNG đụng trước demo (dễ vỡ)

| # | Việc | Vì sao không đụng |
|---|---|---|
| 16 | `Classroom.createdBy: Long` → `@ManyToOne User` | Cần migration DB cẩn thận |
| 17 | Gộp `FundPayment.isPaid + confirmedByAdmin` thành enum status | Đụng FE, ko đáng |
| 18 | Thêm `description` cho Classroom/FundCollection | FE đã hoạt động OK |

---

## 📋 Việc FE cần làm tiếp (gợi ý)

Sau khi BE đã ổn, FE còn các phần có thể polish:

| # | Việc | Effort |
|---|---|---|
| F1 | Trang chi tiết đợt thu (admin click vào card hiện chi tiết + list members) — hiện đã có, chỉ cần kiểm UX | xem lại |
| F2 | Format tiền VNĐ bằng package `intl` thay vì hard-code | 30 phút |
| F3 | UI tổng quan trên home: badge số khoản nợ chưa đóng cho mỗi lớp | 1 tiếng |
| F4 | Pull-to-refresh đã có ở fund/events — kiểm tra tab Expense | 5 phút |
| F5 | Loading skeleton thay vì `CircularProgressIndicator` xấu | optional |
| F6 | Error retry button đã có ở `fund_tab` — replicate cho `events_tab` | 10 phút |
| F7 | Tab Thành viên (khi BE có API #1) | 30 phút |
| F8 | Dashboard tab/màn (khi BE có API #2) | 1-2 tiếng |
| F9 | Test trên thiết bị thật / emulator Android (đổi `baseUrl` → `10.0.2.2`) | 15 phút |
| F10 | Confirm dialog trước khi xác nhận thanh toán / check-in | 10 phút |

---

## 🎯 Trả lời 5 câu hội đồng SẼ hỏi (sau B1–B8)

1. **"Không có token, có vào được API không?"** → 401 JSON, không vào được.
2. **"Admin lớp A có confirm payment lớp B không?"** → Không, `requireAdmin` chặn.
3. **"Member có gọi được API admin không?"** → Không, 403.
4. **"Ai xác nhận khoản này?"** → `payment.confirmedBy` + `confirmedByName`.
5. **"SV join lớp sau khi đã có khoản thu thì sao?"** → Tự sinh payment bổ sung.

Câu thứ 6 nếu bị hỏi: **"QR có tự động không?"** → Trả lời thật: "QR sinh URL VietQR. Sinh viên chuyển khoản. Admin đối chiếu sao kê + bấm xác nhận. Đây là **bán tự động**. Tự động hoàn toàn (webhook ngân hàng / OCR) là hướng phát triển."

---

## 🔗 File tham chiếu

| File | Mục đích |
|---|---|
| `documents/BACKEND_AUDIT.md` | Audit gốc — vấn đề trước khi vá |
| `documents/BACKEND_FIX_LOG.md` | Chi tiết từng file đã sửa B1–B8 |
| `ai-context-md.md` | Tổng quan dự án + conventions + Known Issues |
| `D:/Downloads/classhub_backend_audit_checklist.md` | Checklist gốc theo đề cương |
| `D:/big_dream/classhub_app/documents/FRONTEND_FUND_PROGRESS.md` | Tiến độ FE |

---

## ⚠️ Trước khi demo CHẮC CHẮN phải làm

1. Đổi `vietqr.account-no` trong `application.properties` thành **TK ngân hàng thật** của bạn (hiện đang `109875610620`, `Nguyen Duy Phonggg` — typo "ggg").
2. Đổi `baseUrl` trong 4 service Flutter (`auth_service`, `classroom_service`, `fund_service`, `event_service`) tuỳ thiết bị test (`localhost` / `10.0.2.2` / IP LAN).
3. Smoke test 1 luồng E2E với Postman (xem mục #3 ở "PHẢI làm").
4. Backup DB trước khi chạy lần đầu (Hibernate `ddl-auto=update` sẽ auto-ALTER thêm 2 cột — nên ổn, nhưng backup cho chắc).
