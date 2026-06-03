# Event Analysis - Current Backend

Tài liệu này mô tả module Event theo code hiện tại.

## Flow chính

### Admin tạo sự kiện

1. Client gọi `POST /api/events` với `Authorization: Bearer <adminToken>`.
2. `EventController` lấy `userId` từ `SecurityUtil.currentUserId()`.
3. `EventService.createEvent` gọi `authorizationService.requireAdmin(userId, classroomId)`.
4. Backend lưu `Event`.
5. Response là `EventResponse`.

### Member xem sự kiện lớp

1. Client gọi `GET /api/events/{classroomId}`.
2. Service gọi `requireMember`.
3. Backend trả danh sách event kèm `volunteerCount` và `checkedInCount`.

### Member đăng ký tham gia

1. Client gọi `POST /api/events/{eventId}/volunteer`.
2. Service lấy event, kiểm tra user thuộc lớp của event.
3. Service chặn duplicate bằng `existsByEventIdAndUserId`.
4. Backend lưu `EventParticipant`.

### Admin check-in

1. Client gọi `PUT /api/events/{eventId}/checkin/{userId}`.
2. Service kiểm tra admin thuộc lớp chứa event.
3. Service kiểm tra target user đã đăng ký.
4. Service set `checkedIn`, `checkedInAt`, `checkedBy`.

## Endpoints

| Method | Endpoint | Quyền | Trạng thái |
|---|---|---|---|
| POST | `/api/events` | ADMIN | Xong |
| GET | `/api/events/{classroomId}` | MEMBER | Xong |
| POST | `/api/events/{eventId}/volunteer` | MEMBER | Xong |
| DELETE | `/api/events/{eventId}/volunteer` | OWNER | Xong |
| GET | `/api/events/{eventId}/participants` | ADMIN | Xong |
| PUT | `/api/events/{eventId}/checkin/{userId}` | ADMIN | Xong |
| GET | `/api/events/my/{classroomId}` | MEMBER | Xong |

## Entity

### `events`

| Cột | Ý nghĩa |
|---|---|
| `id` | ID |
| `title` | Tên sự kiện |
| `description` | Mô tả |
| `location` | Địa điểm |
| `event_time` | Thời gian |
| `classroom_id` | Lớp |
| `created_by` | Admin tạo |
| `created_at` | Thời điểm tạo |

### `event_participants`

| Cột | Ý nghĩa |
|---|---|
| `id` | ID |
| `event_id` | Sự kiện |
| `user_id` | Người đăng ký |
| `checked_in` | Đã check-in chưa |
| `checked_in_at` | Thời điểm check-in |
| `checked_by` | Admin check-in |
| `registered_at` | Thời điểm đăng ký |

## Còn thiếu

- Chưa có update/delete event.
- Chưa có assign participant bởi admin.
- Chưa có `attendanceStatus` để phân biệt vắng mặt và chưa check-in.
- Chưa validate `eventTime` phải ở tương lai.
