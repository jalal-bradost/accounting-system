package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.ports.input.service.FiscalPeriodApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.repository.FiscalPeriodRepository;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CompanyContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/companies", produces = "application/json")
public class FiscalPeriodController {

    private final FiscalPeriodApplicationService fiscalPeriodApplicationService;
    private final CompanyContext companyContext;

    public FiscalPeriodController(FiscalPeriodApplicationService fiscalPeriodApplicationService,
                                  CompanyContext companyContext) {
        this.fiscalPeriodApplicationService = fiscalPeriodApplicationService;
        this.companyContext = companyContext;
    }

    @GetMapping("/{companyId}/fiscal-periods")
    @RequiresPermission("accounting.fiscal-period.read")
    public ResponseEntity<List<FiscalPeriodResponse>> listPeriods(@PathVariable UUID companyId) {
        return ResponseEntity.ok(fiscalPeriodApplicationService.listPeriods(companyId).stream()
                .map(FiscalPeriodResponse::from)
                .toList());
    }

    @PostMapping("/{companyId}/fiscal-periods")
    @RequiresPermission("accounting.fiscal-period.write")
    public ResponseEntity<FiscalPeriodResponse> createPeriod(@PathVariable UUID companyId,
                                                              @Valid @RequestBody CreateFiscalPeriodRequest request) {
        FiscalPeriodRepository.FiscalPeriodInfo info = fiscalPeriodApplicationService.createPeriod(
                companyId, request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(FiscalPeriodResponse.from(info));
    }

    @PostMapping("/{companyId}/fiscal-periods/ensure-months")
    @RequiresPermission("accounting.fiscal-period.write")
    public ResponseEntity<List<FiscalPeriodResponse>> ensureMonths(@PathVariable UUID companyId,
                                                                   @Valid @RequestBody YearRequest request) {
        return ResponseEntity.ok(fiscalPeriodApplicationService.ensureMonthlyPeriods(companyId, request.getYear()).stream()
                .map(FiscalPeriodResponse::from)
                .toList());
    }

    @GetMapping("/{companyId}/fiscal-periods/{periodId}/close-checklist")
    @RequiresPermission("accounting.fiscal-period.read")
    public ResponseEntity<CloseChecklistResponse> closeChecklist(@PathVariable UUID companyId,
                                                                 @PathVariable UUID periodId) {
        return ResponseEntity.ok(CloseChecklistResponse.from(
                fiscalPeriodApplicationService.getCloseChecklist(companyId, periodId)));
    }

    @PostMapping("/{companyId}/fiscal-periods/{periodId}/close")
    @RequiresPermission("accounting.fiscal-period.write")
    public ResponseEntity<FiscalPeriodResponse> closeMonth(@PathVariable UUID companyId,
                                                           @PathVariable UUID periodId) {
        UUID closedBy = companyContext.currentUser().map(u -> u.getId()).orElse(null);
        return ResponseEntity.ok(FiscalPeriodResponse.from(
                fiscalPeriodApplicationService.closeMonth(companyId, periodId, closedBy)));
    }

    @PostMapping("/{companyId}/fiscal-periods/close-year")
    @RequiresPermission("accounting.fiscal-period.write")
    public ResponseEntity<List<FiscalPeriodResponse>> closeYear(@PathVariable UUID companyId,
                                                                @Valid @RequestBody YearRequest request) {
        UUID closedBy = companyContext.currentUser().map(u -> u.getId()).orElse(null);
        return ResponseEntity.ok(fiscalPeriodApplicationService.closeYear(companyId, request.getYear(), closedBy).stream()
                .map(FiscalPeriodResponse::from)
                .toList());
    }

    public static class CreateFiscalPeriodRequest {
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class YearRequest {
        @NotNull
        private Integer year;
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }

    public static class FiscalPeriodResponse {
        private final UUID id;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final boolean open;
        private final Instant closedAt;
        private final UUID closedBy;

        public FiscalPeriodResponse(UUID id, LocalDate startDate, LocalDate endDate, boolean open,
                                    Instant closedAt, UUID closedBy) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
            this.open = open;
            this.closedAt = closedAt;
            this.closedBy = closedBy;
        }

        static FiscalPeriodResponse from(FiscalPeriodRepository.FiscalPeriodInfo info) {
            return new FiscalPeriodResponse(
                    info.id(), info.startDate(), info.endDate(), info.open(), info.closedAt(), info.closedBy());
        }

        public UUID getId() { return id; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public boolean isOpen() { return open; }
        public Instant getClosedAt() { return closedAt; }
        public UUID getClosedBy() { return closedBy; }
    }

    public static class CloseChecklistResponse {
        private final UUID periodId;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final boolean open;
        private final int draftJournalEntries;
        private final int draftCustomerInvoices;
        private final int draftVendorBills;
        private final boolean canClose;

        public CloseChecklistResponse(UUID periodId, LocalDate startDate, LocalDate endDate, boolean open,
                                      int draftJournalEntries, int draftCustomerInvoices, int draftVendorBills,
                                      boolean canClose) {
            this.periodId = periodId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.open = open;
            this.draftJournalEntries = draftJournalEntries;
            this.draftCustomerInvoices = draftCustomerInvoices;
            this.draftVendorBills = draftVendorBills;
            this.canClose = canClose;
        }

        static CloseChecklistResponse from(FiscalPeriodApplicationService.CloseChecklist c) {
            return new CloseChecklistResponse(
                    c.periodId(), c.startDate(), c.endDate(), c.open(),
                    c.draftJournalEntries(), c.draftCustomerInvoices(), c.draftVendorBills(), c.canClose());
        }

        public UUID getPeriodId() { return periodId; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public boolean isOpen() { return open; }
        public int getDraftJournalEntries() { return draftJournalEntries; }
        public int getDraftCustomerInvoices() { return draftCustomerInvoices; }
        public int getDraftVendorBills() { return draftVendorBills; }
        public boolean isCanClose() { return canClose; }
    }
}
