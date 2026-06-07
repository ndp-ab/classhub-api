package com.classhub.classhubapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectCheckinSubmissionRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;
}
