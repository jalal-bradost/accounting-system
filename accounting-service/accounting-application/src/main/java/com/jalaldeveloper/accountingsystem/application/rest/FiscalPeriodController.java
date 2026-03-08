package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.FiscalPeriodApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/companies", produces = "application/json")
public class FiscalPeriodController {

    private final FiscalPeriodApplicationService fiscalPeriodApplicationService;

    public FiscalPeriodController(FiscalPeriodApplicationService fiscalPeriodApplicationService) {
        this.fiscalPeriodApplicationService = fiscalPeriodApplicationService;
    }

    @PostMapping("/{companyId}/fiscal-periods")
    public ResponseEntity<FiscalPeriodResponse> createPeriod(@PathVariable UUID companyId,
                                                              @Valid @RequestBody CreateFiscalPeriodRequest request) {
        FiscalPeriodRepository.FiscalPeriodInfo info = fiscalPeriodApplicationService.createPeriod(
                companyId, request.getStartDate(), request.getEndDate(), request.isOpen());
        return ResponseEntity.ok(new FiscalPeriodResponse(info.id(), info.startDate(), info.endDate(), info.open()));
    }

    public static class CreateFiscalPeriodRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean open = true;
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public boolean isOpen() { return open; }
        public void setOpen(boolean open) { this.open = open; }
    }

    public static class FiscalPeriodResponse {
        private final UUID id;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final boolean open;
        public FiscalPeriodResponse(UUID id, LocalDate startDate, LocalDate endDate, boolean open) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
            this.open = open;
        }
        public UUID getId() { return id; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public boolean isOpen() { return open; }
    }
}
