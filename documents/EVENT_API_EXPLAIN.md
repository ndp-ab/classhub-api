# Event API Explain - ClassHub Backend

Tài liệu này giải thích thiết kế Event module theo code backend hiện tại.

## Kiến trúc tổng thể

```text
EventController
    -> nhận HTTP request, lấy userId từ JWT qua SecurityUtil.currentUserId()
EventService
    -> xử lý business logic và gọi AuthorizationService
EventRepository / EventParticipantRepository
    -> truy vấn database qua Spring Data JPA
Event / EventParticipant
    -> mapping bảng MySQL
```

Controller không còn đọc `X-User-Id`. Mọi endpoint event yêu cầu `Authorization: Bearer <token>` vì `SecurityConfig` chỉ `permitAll` cho `/api/auth/**`.

## Vì sao dùng entity relation thay vì raw id?

`Event` lưu:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "classroom_id", nullable = false)
private Classroom classroom;
```

và:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by", nullable = false)
private User createdBy;
```

Lý do:

- Giữ ràng buộc khóa ngoại rõ ràng.
- Nhất quán với các entity khác như `FundCollection`, `ClassMember`.
- Service có thể lấy thông tin liên quan khi cần mapping response.

## Vì sao `EventParticipant` có unique constraint?

```java
@Table(name = "event_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
```

Service đã check duplicate bằng `existsByEventIdAndUserId`, nhưng unique constraint vẫn cần để bảo vệ ở tầng database nếu có hai request đăng ký đồng thời.

## Vì sao dùng `AuthorizationService`?

Các rule phân quyền event không chỉ dựa vào "đã đăng nhập" mà còn phụ thuộc lớp:

- `createEvent`, `getParticipants`, `checkIn`: yêu cầu ADMIN của lớp.
- `getEventsByClassroom`, `volunteer`, `getMyEvents`: yêu cầu thành viên lớp.

Logic này nằm trong `AuthorizationService.requireAdmin/requireMember` để Fund và Event dùng chung, tránh lặp check `ClassMember` ở nhiều service.

## Vì sao `DELETE volunteer` trả `204 No Content`?

Sau khi hủy đăng ký, resource participant không còn nữa. Trả `204` là hợp lý vì không cần body.

## Vì sao `volunteerCount` và `checkedInCount` không lưu trong bảng `events`?

Hai số này được tính từ `event_participants`:

- `volunteerCount = participants.size()`
- `checkedInCount = count(checkedIn == true)`

Không lưu dư giúp tránh lệch dữ liệu khi member hủy đăng ký hoặc admin check-in.

## Vì sao lưu `checkedBy`?

`EventParticipant.checkedBy` cho biết admin nào đã check-in sinh viên. Response trả thêm `checkedByName`, giúp admin/giảng viên truy vết khi kiểm tra dữ liệu điểm danh.

## Câu hỏi bảo vệ thường gặp

**Q: Event API đã phân quyền ADMIN/MEMBER chưa?**

A: Có. Backend lấy user từ JWT và gọi `AuthorizationService`. Member không tạo event, không xem participants, không check-in được.

**Q: User ngoài lớp có đăng ký event được không?**

A: Không. `EventService.volunteer` lấy event, rồi gọi `authorizationService.requireMember(userId, event.getClassroom().getId())`.

**Q: Có phân biệt vắng mặt và chưa check-in không?**

A: Chưa. Code hiện tại chỉ có boolean `checkedIn`. Nếu cần đầy đủ hơn có thể thêm enum `attendanceStatus = PENDING/PRESENT/ABSENT`.

**Q: Có API xóa/sửa event chưa?**

A: Chưa. Backend hiện tại tập trung luồng chính: tạo event, list, volunteer, hủy đăng ký, xem participants, check-in, xem event của mình.
