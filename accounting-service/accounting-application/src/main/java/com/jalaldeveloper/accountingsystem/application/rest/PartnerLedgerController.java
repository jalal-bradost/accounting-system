package com.jalaldeveloper.accountingsystem.application.rest;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service.ReportingApplicationService;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerMovementLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.PartnerLedgerSummaryRow;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Partner subsidiary ledger from posted journal items on receivable/payable accounts (not the same as
 * {@link PartnerStatementController}, which is document-based AR/AP activity for customer communication).
 */
@RestController
@RequestMapping(value = "/api/v1/accounting/partner-ledger", produces = "application/json")
public class PartnerLedgerController {

    private final ReportingApplicationService reportingApplicationService;

    public PartnerLedgerController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = reportingApplicationService;
    }

    @GetMapping
    @RequiresPermission(value = {"accounting.journal-entry.read", "accounting.report.read"},
            op = RequiresPermission.LogicalOp.OR)
    public ResponseEntity<PartnerLedgerResponse> get(@CurrentCompany CompanyId companyId,
                                                       @RequestParam(required = false) UUID partnerId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate from,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate to) {
        PartnerLedgerReport r = reportingApplicationService.getPartnerLedger(companyId.getId(), from, to, partnerId);
        return ResponseEntity.ok(PartnerLedgerResponse.from(r));
    }

    public static final class PartnerLedgerResponse {
        private LocalDate fromDate;
        private LocalDate toDate;
        private UUID partnerId;
        private boolean unpostedDraftJournalEntriesThroughPeriodEnd;
        private List<PartnerLedgerSummaryResponse> summaries;
        private List<PartnerLedgerMovementResponse> lines;

        public static PartnerLedgerResponse from(PartnerLedgerReport r) {
            PartnerLedgerResponse o = new PartnerLedgerResponse();
            o.fromDate = r.fromDate();
            o.toDate = r.toDate();
            o.partnerId = r.partnerId();
            o.unpostedDraftJournalEntriesThroughPeriodEnd = r.unpostedDraftJournalEntriesThroughPeriodEnd();
            o.summaries = r.summaries().stream().map(PartnerLedgerSummaryResponse::from).collect(Collectors.toList());
            o.lines = r.lines().stream().map(PartnerLedgerMovementResponse::from).collect(Collectors.toList());
            return o;
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }

        public UUID getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(UUID partnerId) {
            this.partnerId = partnerId;
        }

        public boolean isUnpostedDraftJournalEntriesThroughPeriodEnd() {
            return unpostedDraftJournalEntriesThroughPeriodEnd;
        }

        public void setUnpostedDraftJournalEntriesThroughPeriodEnd(boolean unpostedDraftJournalEntriesThroughPeriodEnd) {
            this.unpostedDraftJournalEntriesThroughPeriodEnd = unpostedDraftJournalEntriesThroughPeriodEnd;
        }

        public List<PartnerLedgerSummaryResponse> getSummaries() {
            return summaries;
        }

        public void setSummaries(List<PartnerLedgerSummaryResponse> summaries) {
            this.summaries = summaries;
        }

        public List<PartnerLedgerMovementResponse> getLines() {
            return lines;
        }

        public void setLines(List<PartnerLedgerMovementResponse> lines) {
            this.lines = lines;
        }
    }

    public static final class PartnerLedgerSummaryResponse {
        private UUID partnerId;
        private String partnerDisplayName;
        private BigDecimal openingBalance;
        private BigDecimal periodDebit;
        private BigDecimal periodCredit;
        private BigDecimal closingBalance;

        static PartnerLedgerSummaryResponse from(PartnerLedgerSummaryRow s) {
            PartnerLedgerSummaryResponse o = new PartnerLedgerSummaryResponse();
            o.partnerId = s.partnerId();
            o.partnerDisplayName = s.partnerDisplayName();
            o.openingBalance = s.openingBalance();
            o.periodDebit = s.periodDebit();
            o.periodCredit = s.periodCredit();
            o.closingBalance = s.closingBalance();
            return o;
        }

        public UUID getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(UUID partnerId) {
            this.partnerId = partnerId;
        }

        public String getPartnerDisplayName() {
            return partnerDisplayName;
        }

        public void setPartnerDisplayName(String partnerDisplayName) {
            this.partnerDisplayName = partnerDisplayName;
        }

        public BigDecimal getOpeningBalance() {
            return openingBalance;
        }

        public void setOpeningBalance(BigDecimal openingBalance) {
            this.openingBalance = openingBalance;
        }

        public BigDecimal getPeriodDebit() {
            return periodDebit;
        }

        public void setPeriodDebit(BigDecimal periodDebit) {
            this.periodDebit = periodDebit;
        }

        public BigDecimal getPeriodCredit() {
            return periodCredit;
        }

        public void setPeriodCredit(BigDecimal periodCredit) {
            this.periodCredit = periodCredit;
        }

        public BigDecimal getClosingBalance() {
            return closingBalance;
        }

        public void setClosingBalance(BigDecimal closingBalance) {
            this.closingBalance = closingBalance;
        }
    }

    public static final class PartnerLedgerMovementResponse {
        private LocalDate entryDate;
        private UUID journalEntryId;
        private String journalCode;
        private String sequenceNumber;
        private String accountCode;
        private String accountName;
        private String label;
        private BigDecimal debit;
        private BigDecimal credit;
        private BigDecimal balance;
        private UUID reconciliationId;

        static PartnerLedgerMovementResponse from(PartnerLedgerMovementLine l) {
            PartnerLedgerMovementResponse o = new PartnerLedgerMovementResponse();
            o.entryDate = l.entryDate();
            o.journalEntryId = l.journalEntryId();
            o.journalCode = l.journalCode();
            o.sequenceNumber = l.sequenceNumber();
            o.accountCode = l.accountCode();
            o.accountName = l.accountName();
            o.label = l.label();
            o.debit = l.debit();
            o.credit = l.credit();
            o.balance = l.balance();
            o.reconciliationId = l.reconciliationId();
            return o;
        }

        public LocalDate getEntryDate() {
            return entryDate;
        }

        public void setEntryDate(LocalDate entryDate) {
            this.entryDate = entryDate;
        }

        public UUID getJournalEntryId() {
            return journalEntryId;
        }

        public void setJournalEntryId(UUID journalEntryId) {
            this.journalEntryId = journalEntryId;
        }

        public String getJournalCode() {
            return journalCode;
        }

        public void setJournalCode(String journalCode) {
            this.journalCode = journalCode;
        }

        public String getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(String sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        public String getAccountCode() {
            return accountCode;
        }

        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getDebit() {
            return debit;
        }

        public void setDebit(BigDecimal debit) {
            this.debit = debit;
        }

        public BigDecimal getCredit() {
            return credit;
        }

        public void setCredit(BigDecimal credit) {
            this.credit = credit;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public UUID getReconciliationId() {
            return reconciliationId;
        }

        public void setReconciliationId(UUID reconciliationId) {
            this.reconciliationId = reconciliationId;
        }
    }
}
