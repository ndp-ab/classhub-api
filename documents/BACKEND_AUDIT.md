# Backend Audit - Current Status

Tài liệu này là audit hiện tại của `classhub-api` sau khi đã vá JWT, authorization, event và fund flow.

## Kết luận nhanh

Backend hiện tại đã có:

- Auth register/login, BCrypt password, JWT.
- JWT validation thật cho mọi `/api/**` trừ `/api/auth/**`.
- Authorization theo lớp bằng `AuthorizationService.requireMember/requireAdmin`.
- Classroom create/join/my.
- Fund collections, expenses, payments, QR VietQR, member mark-paid, admin confirm.
- Event create/list/volunteer/cancel/participants/check-in/my-events.

## Security hiện tại

| Hạng mục | Trạng thái |
|---|---|
| `/api/auth/**` public | Đúng |
| `/api/**` còn lại yêu cầu JWT | Đúng |
| Không dùng `X-User-Id` tự khai | Đúng |
| User hiện tại lấy từ `SecurityContext` | Đúng |
| Member ngoài lớp bị chặn | Đúng ở Fund/Event/Expense |
| Member gọi API admin bị chặn | Đúng ở Fund/Event/Expense |
| Admin lớp A đụng dữ liệu lớp B bị chặn | Đúng ở Fund/Event/Expense |

## Endpoint hiện có

### Auth

| Method | Endpoint | Ghi chú |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Classroom

| Method | Endpoint | Ghi chú |
|---|---|---|
| POST | `/api/classrooms/create` | Bearer |
| POST | `/api/classrooms/join` | Bearer, tự sinh payment cho khoản thu cũ |
| GET | `/api/classrooms/my` | Bearer |

### Fund

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

### Event

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/api/events` | ADMIN |
| GET | `/api/events/{classroomId}` | MEMBER |
| POST | `/api/events/{eventId}/volunteer` | MEMBER |
| DELETE | `/api/events/{eventId}/volunteer` | OWNER |
| GET | `/api/events/{eventId}/participants` | ADMIN |
| PUT | `/api/events/{eventId}/checkin/{userId}` | ADMIN |
| GET | `/api/events/my/{classroomId}` | MEMBER |

## Những điểm đã xử lý

| Vấn đề cũ | Trạng thái hiện tại |
|---|---|
| API tin `X-User-Id` client tự gửi | Đã bỏ, dùng Bearer JWT |
| `/api/classrooms`, `/api/fund`, `/api/events` public | Đã yêu cầu JWT |
| Không check user thuộc lớp | Đã check bằng `AuthorizationService` |
| Không check role admin | Đã check bằng `requireAdmin` |
| QR/status lộ theo paymentId | Đã owner-only |
| Confirm payment không idempotent | Đã chặn confirm lại |
| Không biết ai confirm payment | Đã có `FundPayment.confirmedBy` và `confirmedByName` |
| Không biết ai check-in event | Đã có `EventParticipant.checkedBy` và `checkedByName` |
| Member join muộn không có debt cũ | Đã sinh payment bổ sung trong `joinClassroom` |

## Rủi ro / còn thiếu

| Mục | Mức ưu tiên | Ghi chú |
|---|---|---|
| `GET /api/classrooms/{id}/members` | Cao | FE tab thành viên cần dữ liệu thật |
| API thống kê quỹ/sự kiện | Cao | Cần cho dashboard/demo |
| Update/delete collection, expense, event | Trung bình | Chưa implement |
| Quản lý role/kick member | Trung bình | Chưa implement |
| `/api/auth/me` | Thấp | Login response hiện đã có user info cơ bản |
| `@Future` cho `eventTime` và deadline validation | Trung bình | Hiện chưa chặn ngày quá khứ |
| `EventParticipant.attendanceStatus` | Trung bình | Hiện chỉ có boolean `checkedIn` |
| Test coverage MockMvc/service | Cao | Hiện mới có context test |

## Verify gần nhất

Đã chạy `mvnw.cmd clean test`: build success, compile 52 source files, 1 test pass.
