# Implementation Summary: Tài khoản ngân hàng theo lớp

## ✅ Đã hoàn thành

### 1. Entity mới
- `ClassroomBankAccount.java` - Entity quản lý tài khoản ngân hàng theo lớp
  - Quan hệ N-1 với Classroom
  - Quan hệ N-1 với User (createdBy)
  - Field `active` để đánh dấu tài khoản đang dùng
  - Validation: bankBin (6 số), accountNo (6-20 số)

### 2. Repository
- `ClassroomBankAccountRepository.java`
  - `findByClassroomIdAndActiveTrue()` - Lấy tài khoản active
  - `findByClassroomIdOrderByCreatedAtDesc()` - Lấy lịch sử
  - `existsByClassroomIdAndActiveTrue()` - Kiểm tra tồn tại

### 3. DTO
- `UpdateClassroomBankAccountRequest.java` - Request tạo/cập nhật
  - Validation: @NotBlank, @Pattern cho bankBin và accountNo
- `ClassroomBankAccountResponse.java` - Response trả về FE
  - Bao gồm thông tin createdByName để audit

### 4. Service
- `ClassroomBankAccountService.java`
  - `getBankAccount()` - Member xem tài khoản hiện tại
  - `getBankAccountHistory()` - Admin xem lịch sử
  - `upsertBankAccount()` - Admin tạo/cập nhật (deactivate cũ + tạo mới)

### 5. Controller
- `ClassroomBankAccountController.java`
  - GET `/api/classrooms/{classroomId}/bank-account` - Xem tài khoản hiện tại
  - GET `/api/classrooms/{classroomId}/bank-account/history` - Xem lịch sử
  - PUT `/api/classrooms/{classroomId}/bank-account` - Tạo/cập nhật
  - Dùng `SecurityUtil.currentUserId()` thay vì `@AuthenticationPrincipal`

### 6. Sửa FundCollectionService
- **createCollection()**: Thêm validation kiểm tra lớp đã có tài khoản chưa
  - Ném BadRequestException nếu chưa có tài khoản
- **generateQr()**: Lấy thông tin tài khoản từ DB thay vì application.properties
  - Lấy `ClassroomBankAccount` active theo classroomId
  - Sinh QR bằng `UriComponentsBuilder` với thông tin từ DB
  - Trả thêm bankName, accountNo, accountName trong QrResponse

### 7. Sửa QrResponse
- Thêm 3 field mới:
  - `bankName` - Tên ngân hàng
  - `accountNo` - Số tài khoản
  - `accountName` - Tên chủ tài khoản

### 8. Sửa application.properties
- Comment/xóa config cũ:
  - `vietqr.bank-bin`
  - `vietqr.account-no`
  - `vietqr.account-name`
- Giữ lại:
  - `vietqr.template=compact2`
- FundCollectionService inject `@Value("${vietqr.template:compact2}")`

## 🧪 Test scenarios cần kiểm tra

### 1. Tạo tài khoản lần đầu
```bash
PUT /api/classrooms/1/bank-account
{
  "bankBin": "970422",
  "bankName": "MB Bank",
  "accountNo": "0123456789",
  "accountName": "NGUYEN VAN A",
  "note": "Tài khoản ban đầu"
}
```
**Kỳ vọng:** 200 OK, tài khoản mới với `active=true`

### 2. Member xem tài khoản khi chưa có
```bash
GET /api/classrooms/1/bank-account
```
**Kỳ vọng:** 400 Bad Request - "Lớp chưa cấu hình tài khoản nhận tiền. Vui lòng liên hệ Admin."

### 3. Admin tạo khoản thu khi chưa có tài khoản
```bash
POST /api/fund/collections
{
  "classroomId": 1,
  "title": "Quỹ lớp tháng 6",
  "amount": 100000,
  "deadline": "2026-06-30"
}
```
**Kỳ vọng:** 400 Bad Request - "Vui lòng cấu hình tài khoản ngân hàng nhận tiền trước khi tạo khoản thu"

### 4. Admin cập nhật tài khoản (lần 2)
```bash
PUT /api/classrooms/1/bank-account
{
  "bankBin": "970415",
  "bankName": "Vietinbank",
  "accountNo": "9876543210",
  "accountName": "TRAN THI B",
  "note": "Đổi thủ quỹ"
}
```
**Kỳ vọng:** 
- 200 OK
- Tài khoản cũ: `active=false`
- Tài khoản mới: `active=true`

### 5. Admin xem lịch sử
```bash
GET /api/classrooms/1/bank-account/history
```
**Kỳ vọng:** Trả về array gồm 2 tài khoản, mới nhất trước:
```json
[
  {
    "id": 2,
    "bankName": "Vietinbank",
    "accountNo": "9876543210",
    "active": true,
    "note": "Đổi thủ quỹ",
    "createdByName": "Admin Name"
  },
  {
    "id": 1,
    "bankName": "MB Bank",
    "accountNo": "0123456789",
    "active": false,
    "note": "Tài khoản ban đầu",
    "createdByName": "Admin Name"
  }
]
```

### 6. Member sinh QR
```bash
GET /api/fund/payments/{paymentId}/qr
```
**Kỳ vọng:** 200 OK, response bao gồm:
```json
{
  "paymentId": 1,
  "qrUrl": "https://img.vietqr.io/image/970415-9876543210-compact2.png?amount=100000&addInfo=QUY1-SV5-1738656000000&accountName=TRAN%20THI%20B",
  "amount": 100000,
  "paymentCode": "QUY1-SV5-1738656000000",
  "collectionTitle": "Quỹ lớp tháng 6",
  "deadline": "2026-06-30",
  "bankName": "Vietinbank",
  "accountNo": "9876543210",
  "accountName": "TRAN THI B"
}
```

### 7. Member xem tài khoản hiện tại
```bash
GET /api/classrooms/1/bank-account
```
**Kỳ vọng:** 200 OK, chỉ thấy tài khoản `active=true`

## 📊 Database Schema

```sql
CREATE TABLE classroom_bank_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    classroom_id BIGINT NOT NULL,
    bank_bin VARCHAR(6) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_no VARCHAR(20) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    note VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_classroom_active (classroom_id, active)
);
```

## 🔍 Kiểm tra compile

```bash
./mvnw clean compile
```

**Kết quả:** ✅ BUILD SUCCESS (không có lỗi, chỉ có warning về deprecated API trong JwtAuthenticationFilter - không liên quan)

## 📝 Notes

1. **Không thay đổi endpoint fund cũ**: Vẫn giữ nguyên `/api/fund/collections`, `/api/fund/payments/{paymentId}/qr`

2. **Backward compatibility**: QrResponse vẫn giữ tất cả field cũ, chỉ thêm 3 field mới

3. **Authorization**: Sử dụng `AuthorizationService.requireMember()` và `requireAdmin()` theo kiến trúc hiện tại

4. **Security**: Controller dùng `SecurityUtil.currentUserId()` thay vì `@AuthenticationPrincipal UserDetails`

5. **Transaction**: Các method thay đổi dữ liệu đều có `@Transactional`

6. **Audit**: Lưu `createdBy` để biết Admin nào tạo/cập nhật tài khoản

7. **Validation**: 
   - `bankBin`: 6 chữ số
   - `accountNo`: 6-20 chữ số
   - `bankName`, `accountName`: không rỗng
   - `note`: optional

## 🚀 Next steps (FE)

1. Thêm màn hình quản lý tài khoản ngân hàng (Admin)
2. Hiển thị thông tin tài khoản trong màn hình QR (Member)
3. Thêm màn hình xem lịch sử thay đổi tài khoản (Admin)
4. Xử lý error 400 khi lớp chưa có tài khoản
