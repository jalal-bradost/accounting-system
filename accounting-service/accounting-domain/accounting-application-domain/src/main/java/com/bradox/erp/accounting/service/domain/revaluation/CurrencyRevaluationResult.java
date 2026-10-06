package com.bradox.erp.accounting.service.domain.revaluation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** What a revaluation of open foreign-currency balances does (or, for a preview, would do). */
public class CurrencyRevaluationResult {

    private boolean preview;
    private LocalDate asOfDate;
    private String baseCurrency;
    private List<Line> lines = new ArrayList<>();
    private BigDecimal totalGain = BigDecimal.ZERO;
    private BigDecimal totalLoss = BigDecimal.ZERO;
    private UUID journalEntryId;
    private UUID reversalJournalEntryId;

    public boolean isPreview() { return preview; }
    public void setPreview(boolean preview) { this.preview = preview; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }
    public BigDecimal getTotalGain() { return totalGain; }
    public void setTotalGain(BigDecimal totalGain) { this.totalGain = totalGain; }
    public BigDecimal getTotalLoss() { return totalLoss; }
    public void setTotalLoss(BigDecimal totalLoss) { this.totalLoss = totalLoss; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getReversalJournalEntryId() { return reversalJournalEntryId; }
    public void setReversalJournalEntryId(UUID reversalJournalEntryId) { this.reversalJournalEntryId = reversalJournalEntryId; }

    /** One account/partner/currency whose carrying value moves to today's rate. */
    public record Line(
            UUID accountId,
            String accountCode,
            String accountName,
            UUID partnerId,
            String partnerName,
            String currencyCode,
            BigDecimal foreignBalance,
            BigDecimal rate,
            BigDecimal carriedValue,
            BigDecimal revaluedValue,
            /** Debit-positive change in the company currency (positive = gain). */
            BigDecimal adjustment) {}
}
