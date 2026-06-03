# Event API - ClassHub Backend

Tài liệu này mô tả Event API theo code backend hiện tại.

## Xác thực và phân quyền

Tất cả endpoint `/api/events/**` yêu cầu:

```http
Authorization: Bearer <token>
```

Backend lấy user hiện tại từ JWT qua `SecurityUtil.currentUserId()`. Không dùng `X-User-Id`.

Phân quyền được kiểm tra trong `EventService` qua `AuthorizationService`:

| Hành động | Quyền |
|---|---|
| Tạo sự kiện | ADMIN của lớp |
| Xem danh sách sự kiện lớp | Thành viên lớp |
| Đăng ký tham gia | Thành viên lớp |
| Hủy đăng ký | Chính user đã đăng ký |
| Xem participants | ADMIN của lớp chứa event |
| Check-in | ADMIN của lớp chứa event |
| Xem event mình đã đăng ký | Thành viên lớp |

## Entity chính

### `Event`

Map với bảng `events`.

| Field | Ý nghĩa |
|---|---|
| `id` | ID sự kiện |
| `title` | Tên sự kiện, bắt buộc |
| `description` | Mô tả, nullable |
| `location` | Địa điểm |
| `eventTime` | Thời gian bắt đầu, bắt buộc |
| `classroom` | Lớp chứa sự kiện |
| `createdBy` | Admin tạo sự kiện |
| `createdAt` | Thời điểm tạo |

### `EventParticipant`

Map với bảng `event_participants`, unique theo `(event_id, user_id)` để chặn đăng ký trùng.

| Field | Ý nghĩa |
|---|---|
| `id` | ID participant |
| `event` | Sự kiện |
| `user` | Người đăng ký |
| `checkedIn` | Đã check-in chưa |
| `checkedInAt` | Thời điểm check-in |
| `checkedBy` | Admin check-in |
| `registeredAt` | Thời điểm đăng ký |

## Response

### `EventResponse`

```json
{
  "id": 1,
  "title": "Họp lớp tháng 6",
  "description": "Tổng kết hoạt động",
  "location": "Phòng A101",
  "eventTime": "2026-06-20T08:00:00",
  "createdByName": "Admin Class",
  "volunteerCount": 2,
  "checkedInCount": 1,
  "createdAt": "2026-06-03T20:00:00"
}
```

`volunteerCount` và `checkedInCount` được tính runtime từ bảng `event_participants`.

### `EventParticipantResponse`

```json
{
  "id": 1,
  "eventId": 1,
  "userId": 2,
  "fullName": "Student One",
  "eventTitle": "Họp lớp tháng 6",
  "checkedIn": true,
  "checkedInAt": "2026-06-20T08:10:00",
  "checkedByName": "Admin Class",
  "registeredAt": "2026-06-03T20:05:00"
}
```

## Endpoints

### Tạo sự kiện

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

Validation:

- `title` không được trống.
- `eventTime` không được null.
- `classroomId` không được null.
- User hiện tại phải là ADMIN của lớp.

### Xem danh sách sự kiện của lớp

```http
GET /api/events/{classroomId}
Authorization: Bearer <token>
```

User phải là thành viên lớp.

### Đăng ký tham gia

```http
POST /api/events/{eventId}/volunteer
Authorization: Bearer <memberToken>
```

User phải thuộc lớp chứa event. Backend chặn duplicate bằng cả service check và DB unique constraint.

### Hủy đăng ký

```http
DELETE /api/events/{eventId}/volunteer
Authorization: Bearer <memberToken>
```

Không được hủy nếu participant đã check-in.

### Admin xem participants

```http
GET /api/events/{eventId}/participants
Authorization: Bearer <adminToken>
```

Admin phải thuộc lớp chứa event.

### Admin check-in

```http
PUT /api/events/{eventId}/checkin/{userId}
Authorization: Bearer <adminToken>
```

Backend:

- Kiểm tra admin thuộc lớp chứa event.
- Kiểm tra target user đã đăng ký event.
- Chặn check-in lần hai.
- Set `checkedIn = true`, `checkedInAt = now`, `checkedBy = admin`.

### Xem event mình đã đăng ký

```http
GET /api/events/my/{classroomId}
Authorization: Bearer <memberToken>
```

User phải là thành viên lớp.

## Lỗi thường gặp

| Status | Khi nào |
|---|---|
| 400 | Event không tồn tại, user chưa đăng ký, đăng ký trùng, check-in trùng |
| 401 | Thiếu token hoặc token sai |
| 403 | User không thuộc lớp hoặc không phải admin |

## Chưa có trong backend hiện tại

- `PUT /api/events/{eventId}` cập nhật event.
- `DELETE /api/events/{eventId}` xóa event.
- `POST /api/events/{eventId}/assign` admin chỉ định participant.
- `attendanceStatus` dạng `PRESENT/ABSENT/PENDING`; hiện tại chỉ có boolean `checkedIn`.
