package com.classhub.classhubapi.controller;

import com.classhub.classhubapi.dto.BankResponse;
import com.classhub.classhubapi.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ResponseEntity<List<BankResponse>> getBanks() {
        return ResponseEntity.ok(bankService.getBanks());
    }

    // MVP limitation: this endpoint is currently protected by JWT only because
    // the project does not have a system-admin role yet. Restrict before production.
        @PostMapping("/sync")
        public ResponseEntity<Map<String, Integer>> syncBanks() {
            int syncedCount = bankService.syncBanksFromVietQr();
            return ResponseEntity.ok(Map.of("syncedCount", syncedCount));
        }
}
