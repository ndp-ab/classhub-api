# Bug Report — Phân hệ Quỹ lớp (Fund Module)

> **Trạng thái:** Chưa sửa  
> **Phát hiện:** 2026-05-14  
> **Mức độ:** 🔴 Nghiêm trọng / 🟡 Logic / 🟢 Nhỏ

---

## 🔴 NHÓM 1 — Security (Ảnh hưởng trực tiếp đến bảo mật)

### BUG-01 — MEMBER tạo được khoản thu
- **File:** `FundCollectionService.java` → `createCollection()`
- **Vấn đề:** Không kiểm tra role của người gọi API trong lớp. Chỉ check `userId` tồn tại.
- **Hậu quả:** Sinh viên bình thường (MEMBER) có thể tạo khoản thu thay Admin.
- **Fix cần làm:** Query `ClassMemberRepository` kiểm tra caller có role `ADMIN` trong lớp đó không.

---

### BUG-02 — Tạo khoản thu cho lớp mình không thuộc
- **File:** `FundCollectionService.java` → `createCollection()`
- **Vấn đề:** Không kiểm tra `userId` có phải thành viên của `classroomId` không. Chỉ check lớp tồn tại.
- **Hậu quả:** Bất kỳ user nào biết `classroomId` của lớp khác đều có thể tạo quỹ cho lớp đó.
- **Fix cần làm:** Cùng query BUG-01 — check `ClassMember` tồn tại với đúng `userId + classroomId`.

---

### BUG-03 — MEMBER tạo được khoản chi (FundExpense)
- **File:** `FundExpenseService.java` → `createExpense()`
- **Vấn đề:** Hoàn toàn giống BUG-01 và BUG-02. Không check role, không check membership.
- **Hậu quả:** Sinh viên có thể ghi khoản chi tùy ý, làm sai lệch số dư quỹ.
- **Fix cần làm:** Inject `ClassMemberRepository`, thêm check tương tự BUG-01.

---

### BUG-04 — Bất kỳ ai confirm thanh toán của người khác
- **File:** `FundCollectionService.java` → `confirmPayment()`
- **Vấn đề:** Không check người gọi có phải ADMIN của lớp chứa khoản thu đó không.
- **Hậu quả:** Sinh viên A có thể confirm thanh toán cho sinh viên B mà không cần là Admin. Hoặc người ngoài lớp confirm được.
- **Fix cần làm:** Thêm `userId` header vào endpoint confirm, kiểm tra role ADMIN qua `ClassMemberRepository`.

---

### BUG-05 — Sinh viên xem QR của người khác
- **File:** `FundCollectionService.java` → `generateQr()`
- **Vấn đề:** Nhận `paymentId` nhưng không kiểm tra `paymentId` đó có thuộc về người đang gọi API không.
- **Hậu quả:** Biết `paymentId` của người khác là có thể lấy mã QR của họ (dù tác hại thực tế thấp vì QR chỉ chứa nội dung chuyển khoản đúng số tài khoản ban cán sự).
- **Fix cần làm:** Thêm `X-User-Id` header vào endpoint `/qr`, so sánh với `payment.getUser().getId()`.

---

### BUG-06 — Polling status của người khác
- **File:** `FundCollectionService.java` → `getPaymentStatus()`
- **Vấn đề:** Tương tự BUG-05. Không validate ownership của `paymentId`.
- **Hậu quả:** Biết `paymentId` là xem được trạng thái thanh toán của bất kỳ ai.
- **Fix cần làm:** Tương tự BUG-05.

---

## 🟡 NHÓM 2 — Logic nghiệp vụ

### BUG-07 — Confirm thanh toán nhiều lần
- **File:** `FundCollectionService.java` → `confirmPayment()`
- **Vấn đề:** Không check `isConfirmedByAdmin == true` trước khi ghi đè. Gọi API confirm 2 lần vẫn không lỗi, chỉ `paidAt` bị cập nhật lại.
- **Hậu quả:** Dữ liệu `paidAt` không chính xác nếu admin bấm nhầm nhiều lần. Có thể gây nhầm lẫn khi đối soát.
- **Fix cần làm:** Thêm check `if (payment.isConfirmedByAdmin()) throw new BadRequestException("Đã xác nhận rồi")`.

---

### BUG-08 — Tạo khoản thu khi lớp chưa có thành viên
- **File:** `FundCollectionService.java` → `createCollection()`
- **Vấn đề:** Nếu lớp không có thành viên nào (`members.size() == 0`), collection vẫn được lưu nhưng không có payment nào được tạo.
- **Hậu quả:** Collection "ma" — tồn tại trong DB nhưng không có ai để thu tiền. Khi thành viên join lớp sau, họ sẽ không có payment cho khoản thu này.
- **Fix cần làm:** Check `members.isEmpty()` → throw lỗi hoặc ít nhất log warning.

---

### BUG-09 — `amount` âm hoặc bằng 0
- **File:** `CreateCollectionRequest.java` (DTO), tương tự `CreateExpenseRequest.java`
- **Vấn đề:** Không có validation `@Positive` hoặc `@DecimalMin` trên field `amount`.
- **Hậu quả:** Có thể tạo khoản thu 0đ hoặc -50,000đ. QR sẽ tạo ra với số tiền âm.
- **Fix cần làm:** Thêm `@Positive(message = "Số tiền phải lớn hơn 0")` vào field `amount` trong DTO.

---

### BUG-10 — `deadline` trong quá khứ
- **File:** `CreateCollectionRequest.java` (DTO)
- **Vấn đề:** Không validate `deadline >= today`.
- **Hậu quả:** Admin có thể tạo khoản thu với hạn chót ngày hôm qua. Không crash nhưng dữ liệu vô nghĩa.
- **Fix cần làm:** Thêm `@FutureOrPresent(message = "Hạn chót phải từ hôm nay trở đi")` vào field `deadline`.

---

## 🟢 NHÓM 3 — Nhỏ (Code Quality)

### BUG-11 — Không check lớp tồn tại khi lấy danh sách khoản thu
- **File:** `FundCollectionService.java` → `getCollectionsByClassroom()`
- **Vấn đề:** Truyền `classroomId` không tồn tại → trả về `[]` thay vì báo lỗi rõ ràng.
- **Fix:** Thêm `classroomRepository.findById().orElseThrow(...)` trước khi query.

---

### BUG-12 — Không check userId hợp lệ khi xem nợ cá nhân
- **File:** `FundCollectionService.java` → `getMyPayments()`
- **Vấn đề:** Truyền `userId` không tồn tại → trả về `[]` thay vì báo lỗi.
- **Fix:** Thêm `userRepository.findById().orElseThrow(...)`.

---

### BUG-13 — `X-User-Id` header không được xác thực (Toàn hệ thống)
- **File:** Toàn bộ Controller
- **Vấn đề:** Bất kỳ ai cũng có thể giả mạo `X-User-Id: 1` để hành động với quyền của user khác (kể cả Admin).
- **Hậu quả:** Đây là root cause của hầu hết các bug Security ở nhóm 1.
- **Fix đúng đắn:** Implement JWT Filter — extract `userId` từ token thay vì tin tưởng header.
- **Ghi chú:** Đây là known issue, đã ghi nhận trong `ai-context-md.md`. Ưu tiên sau khi hoàn thiện tính năng.

---

## Thứ tự ưu tiên sửa

```
Ưu tiên cao (sửa trước demo):
  BUG-01, BUG-02, BUG-03  ← 1 fix chung: thêm check ClassMember + role ADMIN
  BUG-04                   ← thêm userId vào confirm endpoint
  BUG-09, BUG-10           ← thêm annotation validation vào DTO (nhanh nhất)

Ưu tiên trung bình:
  BUG-07                   ← check idempotency khi confirm
  BUG-08                   ← check lớp có member trước khi tạo collection

Ưu tiên thấp (sau khi xong tính năng):
  BUG-05, BUG-06           ← ownership check trên QR và status
  BUG-11, BUG-12           ← thêm validation check tồn tại
  BUG-13                   ← migrate từ X-User-Id sang JWT Filter
```
