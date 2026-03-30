package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassroomRequest {

    @NotBlank(message = "Tên lớp không được để trống")
    private String className;

    private String faculty;

    private String academicYear;
}