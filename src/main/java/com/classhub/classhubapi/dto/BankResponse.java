package com.classhub.classhubapi.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankResponse {

    private Long id;
    private String bankBin;
    private String code;
    private String name;
    private String shortName;
    private String logo;
    private Boolean transferSupported;
    private Boolean lookupSupported;
}
