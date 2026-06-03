# Fund Bug Report - Current Status

Tài liệu này thay thế bug report cũ. Các lỗi liên quan `X-User-Id`, thiếu authorization, QR ownership và confirm idempotency đã được xử lý trong code hiện tại.

## Đã xử lý

| Bug cũ | Trạng thái hiện tại |
|---|---|
| Member có thể tạo khoản thu/khoản chi | Đã chặn bằng `requireAdmin` |
| User ngoài lớp xem collection/expense | Đã chặn bằng `requireMember` |
| Ai biết `paymentId` cũng xem được QR/status | Đã owner-only |
| Admin lớp A confirm payment lớp B | Đã chặn bằng `requireAdmin` trên classroom của payment |
| Confirm payment nhiều lần ghi đè `paidAt` | Đã chặn bằng idempotency guard |
| Không biết ai confirm payment | Đã lưu `confirmedBy` và trả `confirmedByName` |
| Member join muộn không có payment của collection cũ | Đã sinh payment bổ sung khi join lớp |
| `X-User-Id` giả mạo user khác | Đã bỏ, dùng JWT Bearer |

## Flow payment hiện tại

1. Admin tạo collection.
2. Backend tự tạo `FundPayment` cho member.
3. Member lấy QR bằng endpoint owner-only.
4. Member gọi `POST /api/fund/payments/{paymentId}/mark-paid`.
5. Payment chuyển sang `PENDING_VERIFICATION`.
6. Admin đối chiếu sao kê.
7. Admin gọi `PUT /api/fund/payments/{paymentId}/confirm`.
8. Payment chuyển sang `CONFIRMED`.

## Các trạng thái hợp lệ

| Status | Ý nghĩa |
|---|---|
| `UNPAID` | Chưa báo chuyển khoản |
| `PENDING_VERIFICATION` | Member đã báo chuyển khoản, admin chưa xác nhận |
| `CONFIRMED` | Admin đã xác nhận |

## Còn nên làm

| Mục | Lý do |
|---|---|
| API thống kê quỹ | Cần tổng thu/chi/dư/nợ |
| Update/delete collection, expense | Admin cần sửa dữ liệu nhập nhầm |
| Test service/controller cho authorization | Tránh regression security |
| Cấu hình secret/database bằng env var | Tránh hard-code khi deploy thật |
| Validate deadline/amount kỹ hơn | Amount đã có `DecimalMin`, deadline vẫn nên check không ở quá khứ |
