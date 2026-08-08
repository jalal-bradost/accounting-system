package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceCommand;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.create.OpeningBalanceResponse;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.OpeningBalanceApplicationService;
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
        OpeningBalanceCommand command = new OpeningBalanceCommand(
                request.getCompanyId(), request.getDate(), request.getCurrencyCode(), request.isReplace(), lines);
        return ResponseEntity.ok(openingBalanceApplicationService.setOpeningBalances(command));
    }

    public static class OpeningBalanceRequest {
        private UUID companyId;
        private LocalDate date;
        private String currencyCode;
        private boolean replace;
        private List<OpeningBalanceLineRequest> lines;

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
}
