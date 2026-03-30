package com.classhub.classhubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ClassroomResponse {

    private Long id;
    private String className;
    private String faculty;
    private String academicYear;
    private String inviteCode;
    private String role;        // ADMIN hoặc MEMBER
}