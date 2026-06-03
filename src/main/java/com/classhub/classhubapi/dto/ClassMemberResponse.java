package com.classhub.classhubapi.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassMemberResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String role;          // "ADMIN" hoặc "MEMBER"
    private LocalDateTime joinedAt;
}
