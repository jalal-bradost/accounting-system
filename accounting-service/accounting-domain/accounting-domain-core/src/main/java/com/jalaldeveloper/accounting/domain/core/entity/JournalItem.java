package com.jalaldeveloper.accounting.domain.core.entity;

import com.jalaldeveloper.accounting.domain.core.ValueObject.AccountId;
import com.jalaldeveloper.accounting.domain.core.ValueObject.JournalItemId;
import com.jalaldeveloper.accountingsystem.domain.entity.BaseEntity;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Currency;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;

import java.math.BigDecimal;

public class JournalItem extends BaseEntity<JournalItemId> {
    private final AccountId accountId;
    private final String label;
    private final BigDecimal debit;
    private final BigDecimal credit;

    // Multi-currency support (Odoo style)
    private final Money amountCurrency;
    private final Currency currency;

    private JournalItem(Builder builder) {
        super.setId(builder.id);
        accountId = builder.accountId;
        label = builder.label;
        debit = builder.debit;
        credit = builder.credit;
        amountCurrency = builder.amountCurrency;
        currency = builder.currency;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public Money getAmountCurrency() {
        return amountCurrency;
    }

    public Currency getCurrency() {
        return currency;
    }

    public static final class Builder {
        private JournalItemId id;
        private AccountId accountId;
        private String label;
        private BigDecimal debit;
        private BigDecimal credit;
        private Money amountCurrency;
        private Currency currency;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(JournalItemId val) {
            id = val;
            return this;
        }

        public Builder accountId(AccountId val) {
            accountId = val;
            return this;
        }

        public Builder label(String val) {
            label = val;
            return this;
        }

        public Builder debit(BigDecimal val) {
            debit = val;
            return this;
        }

        public Builder credit(BigDecimal val) {
            credit = val;
            return this;
        }

        public Builder amountCurrency(Money val) {
            amountCurrency = val;
            return this;
        }

        public Builder currency(Currency val) {
            currency = val;
            return this;
        }

        public JournalItem build() {
            return new JournalItem(this);
        }
    }
}