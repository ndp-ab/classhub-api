package com.classhub.classhubapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VietQrBanksResponse {

    private String code;
    private String desc;
    private List<BankData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankData {

        private Integer id;
        private String name;
        private String code;
        private String bin;
        private String shortName;
        private String logo;
        private Integer transferSupported;
        private Integer lookupSupported;
    }
}
