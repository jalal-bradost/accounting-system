package com.jalaldeveloper.accounting.domain.core.entity;

import com.jalaldeveloper.accounting.domain.core.ValueObject.JournalEntryId;
import com.jalaldeveloper.accounting.domain.core.ValueObject.JournalEntryStatus;
import com.jalaldeveloper.accounting.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accounting.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.entity.AggergateRoot;
import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class JournalEntry extends AggergateRoot<JournalEntryId> {
    private final CompanyId companyId;
    private final JournalId journalId;
    private final String sequenceNumber;
    private final LocalDate date;
    private final Currency currency;
    private final List<JournalItem> items;
    private JournalEntryStatus status;

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

    public void validate() {
        BigDecimal totalDebit = items.stream().map(JournalItem::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = items.stream().map(JournalItem::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new DomainException("Entry is not balanced! Debits must equal Credits.");
        }
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
