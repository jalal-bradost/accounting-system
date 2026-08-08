package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single opening-balance line. {@code amount} is signed: a positive value is a debit
 * (e.g. assets such as cash/bank/receivable), a negative value is a credit
 * (e.g. liabilities/equity such as payable/owner capital). For receivable/payable
 * accounts a {@code partnerId} is required so the balance flows into the partner ledger.
 */
public class OpeningBalanceLine {
    @NotNull
    private final UUID accountId;
    private final UUID partnerId;
    @NotNull
    private final BigDecimal amount;

    public OpeningBalanceLine(UUID accountId, UUID partnerId, BigDecimal amount) {
        this.accountId = accountId;
        this.partnerId = partnerId;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public UUID getAccountId() { return accountId; }
    public UUID getPartnerId() { return partnerId; }
    public BigDecimal getAmount() { return amount; }
}
