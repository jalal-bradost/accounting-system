package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.PartnerRef;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class JournalEntry extends AggregateRoot<JournalEntryId> {
    private final CompanyId companyId;
    private final JournalId journalId;
    private final String sequenceNumber;
    private final LocalDate date;
    private final Currency currency;
    private final List<JournalItem> items;
    private final JournalEntryId reversalOfEntryId;
    private JournalEntryStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant postedAt;
    private final String postedBy;
    private final PartnerRef partnerRef;

    public void validate() {
        if (items == null || items.size() < 2) {
            throw new AccountingDomainException("Journal entry must have at least two lines.");
        }
        BigDecimal zero = BigDecimal.ZERO;
        for (JournalItem item : items) {
            BigDecimal d = item.getDebit() != null ? item.getDebit() : zero;
            BigDecimal c = item.getCredit() != null ? item.getCredit() : zero;
            boolean hasDebit = d.compareTo(zero) > 0;
            boolean hasCredit = c.compareTo(zero) > 0;
            boolean bothZero = d.compareTo(zero) == 0 && c.compareTo(zero) == 0;
            if (bothZero || (hasDebit && hasCredit)) {
                throw new AccountingDomainException(
                    "Each line must have either debit or credit (not both, not both zero). Invalid line: account=" + item.getAccountId() + " debit=" + d + " credit=" + c);
            }
        }
        BigDecimal totalDebit = items.stream().map(JournalItem::getDebit).reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
        BigDecimal totalCredit = items.stream().map(JournalItem::getCredit).reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new AccountingDomainException("Entry is not balanced! Debits must equal Credits.");
        }
    }

    public void post() {
        if (this.status != JournalEntryStatus.DRAFT) {
            throw new AccountingDomainException("Only Draft entries can be posted.");
        }

        if (items == null || items.isEmpty()) {
            throw new AccountingDomainException("Cannot post an empty journal entry.");
        }

        // Logic from previous step: Ensures Sum(Debit) == Sum(Credit)
        validate();

        this.status = JournalEntryStatus.POSTED;
    }

    public void cancel() {
        if (this.status != JournalEntryStatus.DRAFT) {
            throw new AccountingDomainException("Only DRAFT entries can be cancelled. POSTED entries must be reversed.");
        }
        this.status = JournalEntryStatus.CANCELLED;
    }

    private JournalEntry(Builder builder) {
        super.setId(builder.id);
        companyId = builder.companyId;
        journalId = builder.journalId;
        sequenceNumber = builder.sequenceNumber;
        date = builder.date;
        currency = builder.currency;
        items = builder.items;
        reversalOfEntryId = builder.reversalOfEntryId;
        status = builder.status;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        postedAt = builder.postedAt;
        postedBy = builder.postedBy;
        partnerRef = builder.partnerRef;
    }

    public CompanyId getCompanyId() {
        return companyId;
    }

    public JournalId getJournalId() {
        return journalId;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public Currency getCurrency() {
        return currency;
    }

    public List<JournalItem> getItems() {
        return items;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public JournalEntryId getReversalOfEntryId() {
        return reversalOfEntryId;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPostedAt() { return postedAt; }
    public String getPostedBy() { return postedBy; }
    public PartnerRef getPartnerRef() { return partnerRef; }

    public static Builder builder() {
        return Builder.builder();
    }

    public static final class Builder {
        private JournalEntryId id;
        private CompanyId companyId;
        private JournalId journalId;
        private String sequenceNumber;
        private LocalDate date;
        private Currency currency;
        private List<JournalItem> items;
        private JournalEntryId reversalOfEntryId;
        private JournalEntryStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant postedAt;
        private String postedBy;
        private PartnerRef partnerRef;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(JournalEntryId val) {
            id = val;
            return this;
        }

        public Builder companyId(CompanyId val) {
            companyId = val;
            return this;
        }

        public Builder journalId(JournalId val) {
            journalId = val;
            return this;
        }

        public Builder sequenceNumber(String val) {
            sequenceNumber = val;
            return this;
        }

        public Builder date(LocalDate val) {
            date = val;
            return this;
        }

        public Builder currency(Currency val) {
            currency = val;
            return this;
        }

        public Builder items(List<JournalItem> val) {
            items = val;
            return this;
        }

        public Builder reversalOfEntryId(JournalEntryId val) {
            reversalOfEntryId = val;
            return this;
        }

        public Builder status(JournalEntryStatus val) {
            status = val;
            return this;
        }

        public Builder createdAt(Instant val) { createdAt = val; return this; }
        public Builder updatedAt(Instant val) { updatedAt = val; return this; }
        public Builder postedAt(Instant val) { postedAt = val; return this; }
        public Builder postedBy(String val) { postedBy = val; return this; }
        public Builder partnerRef(PartnerRef val) { partnerRef = val; return this; }

        public JournalEntry build() {
            return new JournalEntry(this);
        }
    }
}
