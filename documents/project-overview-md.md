# ClassHub Backend Overview

Tài liệu này phản ánh backend hiện tại trong `classhub-api`.

## Mục tiêu

ClassHub hỗ trợ quản lý lớp học, quỹ lớp và sự kiện lớp cho ban cán sự/sinh viên.

## Công nghệ

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- JWT với `jjwt`
- Lombok

## Xác thực và phân quyền

`SecurityConfig` hiện tại:

- `POST /api/auth/**`: public.
- Mọi `/api/**` khác: bắt buộc JWT.
- Stateless session.
- `JwtAuthenticationFilter` đọc `Authorization: Bearer <token>`.
- `SecurityUtil.currentUserId()` lấy user hiện tại từ `SecurityContext`.

Backend không dùng `X-User-Id`.

Phân quyền theo lớp nằm trong `AuthorizationService`:

- `requireMember(userId, classroomId)`.
- `requireAdmin(userId, classroomId)`.

## Module đã có

### Auth

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký, hash password bằng BCrypt, trả JWT |
| POST | `/api/auth/login` | Đăng nhập, trả JWT |

### Classroom

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/classrooms/create` | Tạo lớp, sinh invite code, gán người tạo là ADMIN |
| POST | `/api/classrooms/join` | Join lớp bằng invite code, gán MEMBER |
| GET | `/api/classrooms/my` | Danh sách lớp của user hiện tại |

Khi member join lớp muộn, backend tự tạo `FundPayment` cho các khoản thu đã tồn tại trong lớp.

### Fund

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/fund/collections` | ADMIN | Tạo khoản thu và auto-sinh payment |
| GET | `/api/fund/collections/{classroomId}` | MEMBER | Danh sách khoản thu của lớp |
| GET | `/api/fund/collections/{collectionId}/payments` | ADMIN | Danh sách payment của khoản thu |
| PUT | `/api/fund/payments/{paymentId}/confirm` | ADMIN | Admin xác nhận payment |
| POST | `/api/fund/payments/{paymentId}/mark-paid` | OWNER | Member báo đã chuyển khoản |
| GET | `/api/fund/payments/my/{classroomId}` | MEMBER | Nợ/khoản đóng của user hiện tại |
| GET | `/api/fund/payments/{paymentId}/qr` | OWNER | Sinh URL QR VietQR |
| GET | `/api/fund/payments/{paymentId}/status` | OWNER | Polling trạng thái payment |
| POST | `/api/fund/expenses` | ADMIN | Tạo khoản chi |
| GET | `/api/fund/expenses/{classroomId}` | MEMBER | Danh sách khoản chi |

Trạng thái payment:

- `UNPAID`
- `PENDING_VERIFICATION`
- `CONFIRMED`

### Event

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/events` | ADMIN | Tạo sự kiện |
| GET | `/api/events/{classroomId}` | MEMBER | Danh sách sự kiện của lớp |
| POST | `/api/events/{eventId}/volunteer` | MEMBER | Đăng ký tham gia |
| DELETE | `/api/events/{eventId}/volunteer` | OWNER | Hủy đăng ký |
| GET | `/api/events/{eventId}/participants` | ADMIN | Danh sách người đăng ký |
| PUT | `/api/events/{eventId}/checkin/{userId}` | ADMIN | Check-in sinh viên |
| GET | `/api/events/my/{classroomId}` | MEMBER | Event user hiện tại đã đăng ký |

## Bảng dữ liệu chính

- `users`
- `classrooms`
- `class_members`
- `fund_collections`
- `fund_payments`
- `fund_expenses`
- `events`
- `event_participants`

Các cột audit quan trọng hiện có:

- `fund_payments.confirmed_by`: admin xác nhận payment.
- `fund_payments.marked_paid_at`: member báo đã chuyển khoản lúc nào.
- `event_participants.checked_by`: admin check-in.

## QR VietQR

Backend ghép URL ảnh QR từ cấu hình `vietqr.*`, amount trong DB và `paymentCode`. Đây là luồng bán tự động: member chuyển khoản, member báo đã chuyển khoản, admin đối chiếu và xác nhận.

## Còn thiếu / chưa implement

- `GET /api/classrooms/{id}/members`.
- API quản lý role/kick member.
- API thống kê quỹ/sự kiện.
- Update/delete collection, expense, event.
- `POST /api/events/{id}/assign`.
- `/api/auth/me`.
- Webhook ngân hàng/OCR sao kê để tự xác nhận payment.
