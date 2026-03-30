package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinClassroomRequest {

    @NotBlank(message = "Mã tham gia không được để trống")
    private String inviteCode;
}