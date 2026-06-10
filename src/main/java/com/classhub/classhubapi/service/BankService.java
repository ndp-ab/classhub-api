package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.BankResponse;
import com.classhub.classhubapi.dto.VietQrBanksResponse;
import com.classhub.classhubapi.entity.Bank;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${vietqr.banks-url:https://api.vietqr.io/v2/banks}")
    private String vietQrBanksUrl;

    @Transactional(readOnly = true)
    public List<BankResponse> getBanks() {
        return bankRepository.findByActiveTrueOrderByShortNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public int syncBanksFromVietQr() {
        VietQrBanksResponse response = fetchVietQrBanks();
        if (response == null || !"00".equals(response.getCode()) || response.getData() == null) {
            throw new BadRequestException("VietQR trả về dữ liệu ngân hàng không hợp lệ");
        }

        int syncedCount = 0;
        for (VietQrBanksResponse.BankData item : response.getData()) {
            if (item == null || !StringUtils.hasText(item.getBin()) || !StringUtils.hasText(item.getName())) {
                continue;
            }

            Bank bank = bankRepository.findByBin(item.getBin())
                    .orElseGet(() -> {
                        Bank newBank = new Bank();
                        newBank.setBin(item.getBin());
                        newBank.setActive(true);
                        return newBank;
                    });

            bank.setVietQrId(item.getId());
            bank.setCode(item.getCode());
            bank.setName(item.getName());
            bank.setShortName(resolveShortName(item));
            bank.setLogo(item.getLogo());
            bank.setTransferSupported(isSupported(item.getTransferSupported()));
            bank.setLookupSupported(isSupported(item.getLookupSupported()));
            if (bank.getActive() == null) {
                bank.setActive(true);
            }

            bankRepository.save(bank);
            syncedCount++;
        }

        return syncedCount;
    }

    private VietQrBanksResponse fetchVietQrBanks() {
        try {
            return restClient.get()
                    .uri(vietQrBanksUrl)
                    .retrieve()
                    .body(VietQrBanksResponse.class);
        } catch (RestClientException e) {
            throw new BadRequestException("Không thể đồng bộ danh sách ngân hàng từ VietQR");
        }
    }

    private String resolveShortName(VietQrBanksResponse.BankData item) {
        if (StringUtils.hasText(item.getShortName())) {
            return item.getShortName();
        }
        if (StringUtils.hasText(item.getCode())) {
            return item.getCode();
        }
        return item.getName();
    }

    private Boolean isSupported(Integer value) {
        return value != null && value == 1;
    }

    private BankResponse toResponse(Bank bank) {
        return BankResponse.builder()
                .id(bank.getId())
                .bankBin(bank.getBin())
                .code(bank.getCode())
                .name(bank.getName())
                .shortName(bank.getShortName())
                .logo(bank.getLogo())
                .transferSupported(bank.getTransferSupported())
                .lookupSupported(bank.getLookupSupported())
                .build();
    }
}
