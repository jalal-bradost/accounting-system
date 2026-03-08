package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.CompanySettingsApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/companies", produces = "application/json")
public class CompanySettingsController {

    private final CompanySettingsApplicationService companySettingsApplicationService;

    public CompanySettingsController(CompanySettingsApplicationService companySettingsApplicationService) {
        this.companySettingsApplicationService = companySettingsApplicationService;
    }

    @PutMapping("/{companyId}/settings")
    public ResponseEntity<Void> setPeriodLockDate(@PathVariable UUID companyId,
                                                  @Valid @RequestBody PeriodLockDateRequest request) {
        companySettingsApplicationService.setPeriodLockDate(companyId, request.getPeriodLockDate());
        return ResponseEntity.noContent().build();
    }

    public static class PeriodLockDateRequest {
        private java.time.LocalDate periodLockDate;
        public java.time.LocalDate getPeriodLockDate() { return periodLockDate; }
        public void setPeriodLockDate(java.time.LocalDate periodLockDate) { this.periodLockDate = periodLockDate; }
    }
}
