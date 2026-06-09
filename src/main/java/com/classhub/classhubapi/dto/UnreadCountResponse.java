package com.classhub.classhubapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnreadCountResponse {

    private long count;
}
