package com.bradox.erp.application.rest;

import com.bradox.erp.accounting.service.domain.create.OpeningBalanceAdjustmentCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceCommand;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceLine;
import com.bradox.erp.accounting.service.domain.create.OpeningBalanceResponse;
import com.bradox.erp.accounting.service.domain.create.OpeningPartnerLine;
import com.bradox.erp.accounting.service.domain.ports.input.service.OpeningBalanceApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/accounting/opening-balances", produces = "application/json")
public class OpeningBalanceController {

    private final OpeningBalanceApplicationService openingBalanceApplicationService;

    public OpeningBalanceController(OpeningBalanceApplicationService openingBalanceApplicationService) {
        this.openingBalanceApplicationService = openingBalanceApplicationService;
    }

    @PostMapping
    public ResponseEntity<OpeningBalanceResponse> setOpeningBalances(@Valid @RequestBody OpeningBalanceRequest request) {
        List<OpeningBalanceLine> lines = request.getLines() == null ? List.of() : request.getLines().stream()
                .map(l -> new OpeningBalanceLine(l.getAccountId(), l.getPartnerId(), l.getAmount()))
                .collect(Collectors.toList());
        List<OpeningPartnerLine> customerLines = request.getCustomerLines() == null ? List.of()
                : request.getCustomerLines().stream()
                .map(l -> new OpeningPartnerLine(l.getPartnerId(), l.getAmount(), l.getReference(),
                        l.getDate(), l.getDueDate()))
                .collect(Collectors.toList());
        List<OpeningPartnerLine> vendorLines = request.getVendorLines() == null ? List.of()
                : request.getVendorLines().stream()
                .map(l -> new OpeningPartnerLine(l.getPartnerId(), l.getAmount(), l.getReference(),
                        l.getDate(), l.getDueDate()))
                .collect(Collectors.toList());
        OpeningBalanceCommand command = new OpeningBalanceCommand(
                request.getCompanyId(), request.getDate(), request.getCurrencyCode(), request.isReplace(),
                lines, customerLines, vendorLines);
        return ResponseEntity.ok(openingBalanceApplicationService.setOpeningBalances(command));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<OpeningBalanceResponse> postAdjustments(
            @Valid @RequestBody OpeningBalanceAdjustmentRequest request) {
        List<OpeningPartnerLine> customerLines = request.getCustomerLines() == null ? List.of()
                : request.getCustomerLines().stream()
                .map(l -> new OpeningPartnerLine(l.getPartnerId(), l.getAmount(), l.getReference(),
                        l.getDate(), l.getDueDate()))
                .collect(Collectors.toList());
        List<OpeningPartnerLine> vendorLines = request.getVendorLines() == null ? List.of()
                : request.getVendorLines().stream()
                .map(l -> new OpeningPartnerLine(l.getPartnerId(), l.getAmount(), l.getReference(),
                        l.getDate(), l.getDueDate()))
                .collect(Collectors.toList());
        OpeningBalanceAdjustmentCommand command = new OpeningBalanceAdjustmentCommand(
                request.getCompanyId(), request.getDate(), request.getCurrencyCode(),
                customerLines, vendorLines);
        return ResponseEntity.ok(openingBalanceApplicationService.postAdjustments(command));
    }

    public static class OpeningBalanceAdjustmentRequest {
        private UUID companyId;
        private LocalDate date;
        private String currencyCode;
        private List<OpeningPartnerLineRequest> customerLines;
        private List<OpeningPartnerLineRequest> vendorLines;

        public UUID getCompanyId() { return companyId; }
        public void setCompanyId(UUID companyId) { this.companyId = companyId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public List<OpeningPartnerLineRequest> getCustomerLines() { return customerLines; }
        public void setCustomerLines(List<OpeningPartnerLineRequest> customerLines) {
            this.customerLines = customerLines;
        }
        public List<OpeningPartnerLineRequest> getVendorLines() { return vendorLines; }
        public void setVendorLines(List<OpeningPartnerLineRequest> vendorLines) {
            this.vendorLines = vendorLines;
        }
    }

    public static class OpeningBalanceRequest {
        private UUID companyId;
        private LocalDate date;
        private String currencyCode;
        private boolean replace;
        private List<OpeningBalanceLineRequest> lines;
        private List<OpeningPartnerLineRequest> customerLines;
        private List<OpeningPartnerLineRequest> vendorLines;

        public UUID getCompanyId() { return companyId; }
        public void setCompanyId(UUID companyId) { this.companyId = companyId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public boolean isReplace() { return replace; }
        public void setReplace(boolean replace) { this.replace = replace; }
        public List<OpeningBalanceLineRequest> getLines() { return lines; }
        public void setLines(List<OpeningBalanceLineRequest> lines) { this.lines = lines; }
        public List<OpeningPartnerLineRequest> getCustomerLines() { return customerLines; }
        public void setCustomerLines(List<OpeningPartnerLineRequest> customerLines) {
            this.customerLines = customerLines;
        }
        public List<OpeningPartnerLineRequest> getVendorLines() { return vendorLines; }
        public void setVendorLines(List<OpeningPartnerLineRequest> vendorLines) { this.vendorLines = vendorLines; }
    }

    public static class OpeningBalanceLineRequest {
        private UUID accountId;
        private UUID partnerId;
        private BigDecimal amount;

        public UUID getAccountId() { return accountId; }
        public void setAccountId(UUID accountId) { this.accountId = accountId; }
        public UUID getPartnerId() { return partnerId; }
        public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class OpeningPartnerLineRequest {
        private UUID partnerId;
        private BigDecimal amount;
        private String reference;
        private LocalDate date;
        private LocalDate dueDate;

        public UUID getPartnerId() { return partnerId; }
        public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    }
}
