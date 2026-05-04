package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.BalanceSheetReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.GeneralLedgerLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.ProfitAndLossReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/companies", produces = "application/json")
public class FinancialReportsController {

    private final ReportingApplicationService reportingApplicationService;

    public FinancialReportsController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = reportingApplicationService;
    }

    @GetMapping("/{companyId}/balance-sheet")
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        BalanceSheetReport report = reportingApplicationService.getBalanceSheet(companyId, asOf);
        return ResponseEntity.ok(BalanceSheetResponse.from(companyId, report));
    }

    @GetMapping("/{companyId}/profit-and-loss")
    public ResponseEntity<ProfitAndLossResponse> getProfitAndLoss(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ProfitAndLossReport report = reportingApplicationService.getProfitAndLoss(companyId, from, to);
        return ResponseEntity.ok(ProfitAndLossResponse.from(companyId, report));
    }

    @GetMapping("/{companyId}/general-ledger")
    public ResponseEntity<GeneralLedgerResponse> getGeneralLedger(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID accountId) {
        List<GeneralLedgerLine> lines = reportingApplicationService.getGeneralLedger(companyId, from, to, accountId);
        List<GeneralLedgerResponse.Line> out = lines.stream()
                .map(l -> new GeneralLedgerResponse.Line(
                        l.accountId(), l.journalEntryId(), l.entryDate(), l.journalCode(), l.sequenceNumber(),
                        l.label(), l.debit(), l.credit(), l.runningBalance()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new GeneralLedgerResponse(companyId, from, to, accountId, out));
    }

    public static class BalanceSheetResponse {
        private final UUID companyId;
        private final LocalDate asOf;
        private final List<Line> assets;
        private final List<Line> liabilities;
        private final List<Line> equity;
        private final java.math.BigDecimal totalAssets;
        private final java.math.BigDecimal totalLiabilities;
        private final java.math.BigDecimal totalEquity;

        public static BalanceSheetResponse from(UUID companyId, BalanceSheetReport r) {
            return new BalanceSheetResponse(
                    companyId,
                    r.asOf(),
                    mapLines(r.assets()),
                    mapLines(r.liabilities()),
                    mapLines(r.equity()),
                    r.totalAssets(),
                    r.totalLiabilities(),
                    r.totalEquity());
        }

        private static List<Line> mapLines(List<BalanceSheetReport.AccountLine> lines) {
            return lines.stream().map(l -> new Line(l.accountId(), l.amount())).collect(Collectors.toList());
        }

        public BalanceSheetResponse(UUID companyId, LocalDate asOf, List<Line> assets, List<Line> liabilities,
                                    List<Line> equity, java.math.BigDecimal totalAssets,
                                    java.math.BigDecimal totalLiabilities, java.math.BigDecimal totalEquity) {
            this.companyId = companyId;
            this.asOf = asOf;
            this.assets = assets;
            this.liabilities = liabilities;
            this.equity = equity;
            this.totalAssets = totalAssets;
            this.totalLiabilities = totalLiabilities;
            this.totalEquity = totalEquity;
        }

        public UUID getCompanyId() { return companyId; }
        public LocalDate getAsOf() { return asOf; }
        public List<Line> getAssets() { return assets; }
        public List<Line> getLiabilities() { return liabilities; }
        public List<Line> getEquity() { return equity; }
        public java.math.BigDecimal getTotalAssets() { return totalAssets; }
        public java.math.BigDecimal getTotalLiabilities() { return totalLiabilities; }
        public java.math.BigDecimal getTotalEquity() { return totalEquity; }

        public static class Line {
            private final UUID accountId;
            private final java.math.BigDecimal amount;
            public Line(UUID accountId, java.math.BigDecimal amount) {
                this.accountId = accountId;
                this.amount = amount;
            }
            public UUID getAccountId() { return accountId; }
            public java.math.BigDecimal getAmount() { return amount; }
        }
    }

    public static class ProfitAndLossResponse {
        private final UUID companyId;
        private final LocalDate from;
        private final LocalDate to;
        private final List<Line> revenue;
        private final List<Line> expenses;
        private final java.math.BigDecimal totalRevenue;
        private final java.math.BigDecimal totalExpenses;
        private final java.math.BigDecimal netIncome;

        public static ProfitAndLossResponse from(UUID companyId, ProfitAndLossReport r) {
            return new ProfitAndLossResponse(
                    companyId,
                    r.from(),
                    r.to(),
                    r.revenue().stream().map(l -> new Line(l.accountId(), l.amount())).collect(Collectors.toList()),
                    r.expenses().stream().map(l -> new Line(l.accountId(), l.amount())).collect(Collectors.toList()),
                    r.totalRevenue(),
                    r.totalExpenses(),
                    r.netIncome());
        }

        public ProfitAndLossResponse(UUID companyId, LocalDate from, LocalDate to, List<Line> revenue, List<Line> expenses,
                                     java.math.BigDecimal totalRevenue, java.math.BigDecimal totalExpenses,
                                     java.math.BigDecimal netIncome) {
            this.companyId = companyId;
            this.from = from;
            this.to = to;
            this.revenue = revenue;
            this.expenses = expenses;
            this.totalRevenue = totalRevenue;
            this.totalExpenses = totalExpenses;
            this.netIncome = netIncome;
        }

        public UUID getCompanyId() { return companyId; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        public List<Line> getRevenue() { return revenue; }
        public List<Line> getExpenses() { return expenses; }
        public java.math.BigDecimal getTotalRevenue() { return totalRevenue; }
        public java.math.BigDecimal getTotalExpenses() { return totalExpenses; }
        public java.math.BigDecimal getNetIncome() { return netIncome; }

        public static class Line {
            private final UUID accountId;
            private final java.math.BigDecimal amount;
            public Line(UUID accountId, java.math.BigDecimal amount) {
                this.accountId = accountId;
                this.amount = amount;
            }
            public UUID getAccountId() { return accountId; }
            public java.math.BigDecimal getAmount() { return amount; }
        }
    }

    public static class GeneralLedgerResponse {
        private final UUID companyId;
        private final LocalDate from;
        private final LocalDate to;
        private final UUID accountId;
        private final List<Line> lines;

        public GeneralLedgerResponse(UUID companyId, LocalDate from, LocalDate to, UUID accountId, List<Line> lines) {
            this.companyId = companyId;
            this.from = from;
            this.to = to;
            this.accountId = accountId;
            this.lines = lines;
        }

        public UUID getCompanyId() { return companyId; }
        public LocalDate getFrom() { return from; }
        public LocalDate getTo() { return to; }
        public UUID getAccountId() { return accountId; }
        public List<Line> getLines() { return lines; }

        public static class Line {
            private final UUID accountId;
            private final UUID journalEntryId;
            private final LocalDate entryDate;
            private final String journalCode;
            private final String sequenceNumber;
            private final String label;
            private final java.math.BigDecimal debit;
            private final java.math.BigDecimal credit;
            private final java.math.BigDecimal runningBalance;

            public Line(UUID accountId, UUID journalEntryId, LocalDate entryDate, String journalCode,
                        String sequenceNumber, String label, java.math.BigDecimal debit, java.math.BigDecimal credit,
                        java.math.BigDecimal runningBalance) {
                this.accountId = accountId;
                this.journalEntryId = journalEntryId;
                this.entryDate = entryDate;
                this.journalCode = journalCode;
                this.sequenceNumber = sequenceNumber;
                this.label = label;
                this.debit = debit;
                this.credit = credit;
                this.runningBalance = runningBalance;
            }

            public UUID getAccountId() { return accountId; }
            public UUID getJournalEntryId() { return journalEntryId; }
            public LocalDate getEntryDate() { return entryDate; }
            public String getJournalCode() { return journalCode; }
            public String getSequenceNumber() { return sequenceNumber; }
            public String getLabel() { return label; }
            public java.math.BigDecimal getDebit() { return debit; }
            public java.math.BigDecimal getCredit() { return credit; }
            public java.math.BigDecimal getRunningBalance() { return runningBalance; }
        }
    }
}
