package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/companies", produces = "application/json")
public class TrialBalanceController {

    private final ReportingApplicationService reportingApplicationService;

    public TrialBalanceController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = reportingApplicationService;
    }

    @GetMapping("/{companyId}/trial-balance")
    public ResponseEntity<TrialBalanceResponse> getTrialBalance(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AccountBalanceRepository.AccountBalanceLine> lines = reportingApplicationService.getTrialBalance(companyId, from, to);
        List<TrialBalanceResponse.Line> responseLines = lines.stream()
                .map(l -> new TrialBalanceResponse.Line(l.accountId(), l.balance()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new TrialBalanceResponse(companyId, from, to, responseLines));
    }

    public static class TrialBalanceResponse {
        private final UUID companyId;
        private final LocalDate from;
        private final LocalDate to;
        private final List<Line> lines;

        public TrialBalanceResponse(UUID companyId, LocalDate from, LocalDate to, List<Line> lines) {
            this.companyId = companyId;
            this.from = from;
            this.to = to;
            this.lines = lines;
        }
        public UUID getCompanyId() { return companyId; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        public List<Line> getLines() { return lines; }

        public static class Line {
            private final UUID accountId;
            private final java.math.BigDecimal balance;
            public Line(UUID accountId, java.math.BigDecimal balance) {
                this.accountId = accountId;
                this.balance = balance;
            }
            public UUID getAccountId() { return accountId; }
            public java.math.BigDecimal getBalance() { return balance; }
        }
    }
}
