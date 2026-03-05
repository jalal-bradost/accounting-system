package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class JournalEntry extends AggregateRoot<JournalEntryId> {
    private final CompanyId companyId;
    private final JournalId journalId;
    private final String sequenceNumber;
    private final LocalDate date;
    private final Currency currency;
    private final List<JournalItem> items;
    private JournalEntryStatus status;

    public void validate() {
        BigDecimal totalDebit = items.stream().map(JournalItem::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = items.stream().map(JournalItem::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);

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
        status = builder.status;
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
        private JournalEntryStatus status;

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

        public Builder status(JournalEntryStatus val) {
            status = val;
            return this;
        }

        public JournalEntry build() {
            return new JournalEntry(this);
        }
    }
}
