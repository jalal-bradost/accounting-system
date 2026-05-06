package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class JournalEntryResponse {
    private final UUID id;
    private final UUID companyId;
    private final UUID journalId;
    private final String sequenceNumber;
    private final LocalDate date;
    private final String currencyCode;
    private final JournalEntryStatus status;
    private final UUID reversalOfEntryId;
    private final List<JournalItemResponse> items;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant postedAt;
    private final String postedBy;
    private final UUID partnerId;
    private final String partnerName;

    public JournalEntryResponse(UUID id, UUID companyId, UUID journalId, String sequenceNumber,
                                LocalDate date, String currencyCode, JournalEntryStatus status,
                                UUID reversalOfEntryId, List<JournalItemResponse> items,
                                Instant createdAt, Instant updatedAt, Instant postedAt, String postedBy) {
        this(id, companyId, journalId, sequenceNumber, date, currencyCode, status,
                reversalOfEntryId, items, createdAt, updatedAt, postedAt, postedBy, null, null);
    }

    public JournalEntryResponse(UUID id, UUID companyId, UUID journalId, String sequenceNumber,
                                LocalDate date, String currencyCode, JournalEntryStatus status,
                                UUID reversalOfEntryId, List<JournalItemResponse> items,
                                Instant createdAt, Instant updatedAt, Instant postedAt, String postedBy,
                                UUID partnerId, String partnerName) {
        this.id = id;
        this.companyId = companyId;
        this.journalId = journalId;
        this.sequenceNumber = sequenceNumber;
        this.date = date;
        this.currencyCode = currencyCode;
        this.status = status;
        this.reversalOfEntryId = reversalOfEntryId;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.postedAt = postedAt;
        this.postedBy = postedBy;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getJournalId() { return journalId; }
    public String getSequenceNumber() { return sequenceNumber; }
    public LocalDate getDate() { return date; }
    public String getCurrencyCode() { return currencyCode; }
    public JournalEntryStatus getStatus() { return status; }
    public UUID getReversalOfEntryId() { return reversalOfEntryId; }
    public List<JournalItemResponse> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPostedAt() { return postedAt; }
    public String getPostedBy() { return postedBy; }
    public UUID getPartnerId() { return partnerId; }
    public String getPartnerName() { return partnerName; }

    public static class JournalItemResponse {
        private final UUID id;
        private final UUID accountId;
        private final String label;
        private final BigDecimal debit;
        private final BigDecimal credit;
        private final String currencyCode;
        private final BigDecimal amountCurrency;
        private final UUID reconciliationId;
        private final UUID partnerId;
        private final String partnerName;

        public JournalItemResponse(UUID id, UUID accountId, String label, BigDecimal debit, BigDecimal credit,
                                   String currencyCode, BigDecimal amountCurrency, UUID reconciliationId) {
            this(id, accountId, label, debit, credit, currencyCode, amountCurrency, reconciliationId, null, null);
        }
        public JournalItemResponse(UUID id, UUID accountId, String label, BigDecimal debit, BigDecimal credit,
                                   String currencyCode, BigDecimal amountCurrency, UUID reconciliationId,
                                   UUID partnerId, String partnerName) {
            this.id = id;
            this.accountId = accountId;
            this.label = label;
            this.debit = debit;
            this.credit = credit;
            this.currencyCode = currencyCode;
            this.amountCurrency = amountCurrency;
            this.reconciliationId = reconciliationId;
            this.partnerId = partnerId;
            this.partnerName = partnerName;
        }
        public UUID getId() { return id; }
        public UUID getAccountId() { return accountId; }
        public String getLabel() { return label; }
        public BigDecimal getDebit() { return debit; }
        public BigDecimal getCredit() { return credit; }
        public String getCurrencyCode() { return currencyCode; }
        public BigDecimal getAmountCurrency() { return amountCurrency; }
        public UUID getReconciliationId() { return reconciliationId; }
        public UUID getPartnerId() { return partnerId; }
        public String getPartnerName() { return partnerName; }
    }
}
