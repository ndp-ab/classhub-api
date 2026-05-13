package com.classhub.classhubapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CollectionResponse {
    private Long id;
    private String title;
    private BigDecimal amount;
    private LocalDate deadline;
    private String createdByName;
    private int totalMembers;
    private int paidCount;
    private LocalDateTime createdAt;
}
