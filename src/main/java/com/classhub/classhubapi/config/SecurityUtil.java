package com.classhub.classhubapi.config;

import com.classhub.classhubapi.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Helper lấy userId hiện tại từ SecurityContext (do JwtAuthenticationFilter set).
// Dùng trong controller thay cho @RequestHeader("X-User-Id").
public final class SecurityUtil {

    private SecurityUtil() {}

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ForbiddenException("Chưa đăng nhập");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) return id;
        throw new ForbiddenException("Phiên đăng nhập không hợp lệ");
    }
}
