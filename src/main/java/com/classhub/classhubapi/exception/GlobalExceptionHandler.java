package com.classhub.classhubapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — lỗi nghiệp vụ chủ động ném
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    // 403 — đã đăng nhập nhưng không có quyền (B2 sẽ dùng nhiều)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN, e.getMessage()));
    }

    // 400 — validate fail trên @Valid DTO, trả chi tiết field nào sai
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400 — thiếu request header bắt buộc (vd thiếu Authorization khi gọi /api)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, "Thiếu header: " + e.getHeaderName()));
    }

    // 400 — body JSON parse fail
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ"));
    }

    // 500 — fallback: tránh trả stacktrace ra client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e) {
        // Log nội bộ — ở đây in stacktrace để debug, production nên dùng logger
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi máy chủ"));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> b = new HashMap<>();
        b.put("status", status.value());
        b.put("message", message);
        return b;
    }
}
