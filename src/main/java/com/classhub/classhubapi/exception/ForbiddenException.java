package com.classhub.classhubapi.exception;

// Ném khi user đã đăng nhập nhưng không có quyền thao tác (member gọi API admin,
// admin lớp A đụng dữ liệu lớp B, v.v.). Khác với BadRequestException (400).
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
