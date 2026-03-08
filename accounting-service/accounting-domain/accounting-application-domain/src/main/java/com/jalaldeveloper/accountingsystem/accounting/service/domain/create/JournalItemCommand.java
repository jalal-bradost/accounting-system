package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class JournalItemCommand {
    @NotNull
    private final UUID accountId;
    private final String label;
    @NotNull
    private final BigDecimal debit;
    @NotNull
    private final BigDecimal credit;
    private final String currencyCode;
    private final BigDecimal amountCurrency;

    public JournalItemCommand(UUID accountId, String label, BigDecimal debit, BigDecimal credit,
                              String currencyCode, BigDecimal amountCurrency) {
        this.accountId = accountId;
        this.label = label;
        this.debit = debit != null ? debit : BigDecimal.ZERO;
        this.credit = credit != null ? credit : BigDecimal.ZERO;
        this.currencyCode = currencyCode;
        this.amountCurrency = amountCurrency;
    }

    public UUID getAccountId() { return accountId; }
    public String getLabel() { return label; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getCredit() { return credit; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getAmountCurrency() { return amountCurrency; }
}
