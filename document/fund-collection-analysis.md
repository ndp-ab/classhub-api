# Phân tích nghiệp vụ: Khoản thu quỹ lớp (FundCollection)

## 1. Bài toán thực tế

Ban cán sự lớp 64KTPM3 cần thu quỹ tháng 4, mỗi người 50.000đ, hạn đóng 30/04. Lớp có 40 sinh viên.

**Cách làm hiện tại (Zalo + Excel):**
- Ban cán sự gửi tin nhắn vào group Zalo: "Nộp quỹ tháng 4, 50k/người, hạn 30/04"
- Tin nhắn bị trôi sau vài giờ, nhiều người không thấy
- Ban cán sự phải nhắn riêng từng người nhắc nợ
- Ghi sổ bằng Excel, dễ nhầm, dễ tranh cãi "tôi đã đóng rồi mà"

**Cách làm trên ClassHub:**
- Admin tạo khoản thu trên app → hệ thống tự tạo danh sách 40 người, ai cũng thấy
- Sinh viên mở app → thấy rõ mình đang nợ khoản nào, hạn bao giờ
- Sinh viên bấm vào khoản thu → hiện QR chuyển khoản chuẩn VietQR
- Chuyển khoản xong → ban cán sự tick xác nhận trên app
- Mọi thứ minh bạch, có lịch sử, không ai tranh cãi

---

## 2. Các actor và hành động

### Admin (Ban cán sự)
- Tạo khoản thu mới (tên, số tiền, hạn đóng)
- Xem danh sách ai đã đóng / chưa đóng
- Tick xác nhận "Đã đóng" cho từng sinh viên
- Xem tổng số tiền đã thu được

### Member (Sinh viên)
- Xem danh sách các khoản thu của lớp
- Xem trạng thái nợ cá nhân (đang nợ khoản nào, bao nhiêu tiền)
- Bấm vào khoản thu → hiện QR VietQR để chuyển khoản
- Xem lịch sử đóng quỹ của mình

---

## 3. Luồng nghiệp vụ chi tiết

### Luồng 1: Admin tạo khoản thu
```
Admin bấm "Tạo khoản thu"
    → Nhập: tên ("Quỹ tháng 4"), số tiền (50000), hạn (30/04/2026)
    → Gửi API
    → Backend:
        1. Lưu khoản thu vào bảng fund_collections
        2. Query bảng class_members lấy tất cả thành viên lớp
        3. Tạo 1 bản ghi fund_payments cho MỖI thành viên
           (isPaid = false, confirmedByAdmin = false)
    → Trả về: CollectionResponse (kèm totalMembers, paidCount)
```

### Luồng 2: Sinh viên xem nợ và đóng quỹ
```
Sinh viên mở tab Quỹ lớp
    → App gọi API lấy danh sách khoản thu
    → Hiển thị: "Quỹ tháng 4 — 50.000đ — Hạn 30/04 — CHƯA ĐÓNG"
    → Sinh viên bấm vào khoản thu
    → App sinh QR VietQR chứa:
        - Số tài khoản ngân hàng (của ban cán sự)
        - Số tiền: 50000
        - Nội dung: "QUY-1-SV2251172450" (mã khoản thu + mã sinh viên)
    → Sinh viên quét QR bằng app ngân hàng, chuyển khoản
```

### Luồng 3: Admin xác nhận đóng quỹ
```
Admin mở danh sách khoản thu "Quỹ tháng 4"
    → Thấy danh sách 40 sinh viên + trạng thái
    → Đối soát với sao kê ngân hàng (nội dung chứa mã sinh viên)
    → Tick xác nhận cho từng người đã đóng
    → Backend: cập nhật isPaid = true, confirmedByAdmin = true, paidAt = now()
```

---

## 4. Thiết kế dữ liệu

### Bảng fund_collections (khoản thu)
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| title | varchar | Tên khoản thu ("Quỹ tháng 4") |
| amount | decimal | Số tiền mỗi người (50000) |
| classroom_id | bigint FK → classrooms | Thuộc lớp nào |
| created_by | bigint FK → users | Admin nào tạo |
| deadline | date | Hạn đóng (30/04/2026) |
| created_at | datetime | Ngày tạo |

### Bảng fund_payments (trạng thái đóng tiền từng người)
| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | bigint PK | Khóa chính |
| user_id | bigint FK → users | Sinh viên nào |
| collection_id | bigint FK → fund_collections | Đóng cho khoản nào |
| is_paid | boolean | Đã đóng chưa (mặc định false) |
| confirmed_by_admin | boolean | Admin xác nhận chưa (mặc định false) |
| paid_at | datetime | Thời gian đóng (null nếu chưa đóng) |

### Mối quan hệ
- 1 khoản thu → nhiều bản ghi đóng tiền (1 bản ghi / 1 sinh viên)
- 1 sinh viên → nhiều bản ghi đóng tiền (mỗi khoản thu 1 bản ghi)
- Khoản thu thuộc về 1 lớp, lớp có nhiều khoản thu

---

## 5. API đã triển khai ✅

| Method | Endpoint | Mô tả | Ai dùng | Trạng thái |
|--------|----------|-------|---------|------------|
| POST | /api/fund/collections | Tạo khoản thu + tự tạo payment cho tất cả thành viên | Admin | ✅ Xong |
| GET | /api/fund/collections/{classroomId} | Xem danh sách khoản thu của lớp | Tất cả | ✅ Xong |
| GET | /api/fund/collections/{collectionId}/payments | Xem ai đã đóng / chưa đóng | Admin | ✅ Xong |
| PUT | /api/fund/payments/{paymentId}/confirm | Xác nhận đã đóng cho 1 sinh viên | Admin | ✅ Xong |
| GET | /api/fund/payments/my/{classroomId} | Xem nợ cá nhân của mình trong lớp | Member | ✅ Xong |

---

## 6. Code đã viết (Backend)

### DTOs
| File | Mô tả |
|------|-------|
| `CreateCollectionRequest.java` | Input: title, amount, classroomId, deadline |
| `CollectionResponse.java` | Output: id, title, amount, deadline, createdByName, totalMembers, paidCount, createdAt |
| `PaymentResponse.java` | Output: id, userId, fullName, collectionTitle, isPaid, confirmedByAdmin, paidAt |

### Repository
| File | Query methods |
|------|--------------|
| `FundCollectionRepository.java` | `findByClassroomId`, `existsByIdAndClassroomId` |
| `FundPaymentRepository.java` | `findByFundCollectionId`, `findByUserIdAndFundCollection_ClassroomId`, `existsByUserIdAndFundCollectionId`, `sumAmountByFundCollectionIdAndConfirmedByAdminTrue` |

### Service — `FundCollectionService.java`
| Method | Logic |
|--------|-------|
| `createCollection()` | Lưu khoản thu → tự tạo payment cho từng member, dùng `@Transactional` |
| `getCollectionsByClassroom()` | Lấy danh sách + tính paidCount động |
| `getPaymentsByCollection()` | Lấy trạng thái đóng tiền từng người |
| `getMyPayments()` | Lấy nợ cá nhân theo userId + classroomId |
| `confirmPayment()` | Set isPaid, confirmedByAdmin, paidAt = now() |
| `toCollectionResponse()` | Helper: entity → CollectionResponse |
| `toPaymentResponse()` | Helper: entity → PaymentResponse |

### Controller — `FundCollectionController.java`
- Mapping đầy đủ 5 endpoints vào `@RequestMapping("/api/fund")`
- Nhận `X-User-Id` header để xác định người dùng (tạm thời, sẽ thay bằng JWT filter)

---

## 7. Điểm phức tạp kỹ thuật (điểm nhấn đồ án)

### 7.1 Tự động tạo payment cho tất cả thành viên
Khi admin tạo khoản thu, backend phải:
- Query bảng class_members lấy tất cả user trong lớp
- Tạo N bản ghi fund_payments (N = số thành viên)
- Dùng @Transactional để đảm bảo hoặc tạo hết hoặc không tạo gì
- Kiểm tra `existsByUserIdAndFundCollectionId` để tránh tạo duplicate

### 7.2 QR VietQR
- Sinh mã QR chuyển khoản theo chuẩn VietQR (napas247)
- Nội dung chuyển khoản chứa mã khoản thu + mã sinh viên
- **Thực hiện ở phía Flutter, không cần backend** (chưa làm)

### 7.3 Tính tổng quỹ
- Tổng thu = SUM(amount) từ fund_payments WHERE confirmedByAdmin = true
- Tổng chi = SUM(amount) từ fund_expenses
- Số dư = Tổng thu - Tổng chi
- Tính runtime, không lưu sẵn (single source of truth)

---

## 7. Câu hỏi hội đồng có thể hỏi

**Q: Tại sao dùng BigDecimal mà không dùng double cho số tiền?**
A: double có lỗi làm tròn (0.1 + 0.2 = 0.30000000000000004), BigDecimal tính chính xác.

**Q: Tại sao không lưu sẵn tổng quỹ vào database?**
A: Để đảm bảo single source of truth — tính từ dữ liệu gốc luôn chính xác, không bị lệch.

**Q: Sinh viên join lớp sau khi khoản thu đã tạo thì sao?**
A: Đây là edge case cần xử lý — có thể tự động tạo payment khi join, hoặc chỉ tính cho những người có mặt lúc tạo khoản thu.

**Q: Admin tick xác nhận sai thì sao?**
A: Có thể thêm nút "Hủy xác nhận" để đổi lại trạng thái.