package com.jalaldeveloper.accountingsystem.accounting.service.domain.create;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of setting opening balances: the posted journal entry plus the amount that was
 * automatically plugged to the Opening Balance Equity account (zero when the input already
 * balanced).
 */
public class OpeningBalanceResponse {
    private final UUID journalEntryId;
    private final BigDecimal openingBalanceEquityAmount;
    private final String message;

    public OpeningBalanceResponse(UUID journalEntryId, BigDecimal openingBalanceEquityAmount, String message) {
        this.journalEntryId = journalEntryId;
        this.openingBalanceEquityAmount = openingBalanceEquityAmount;
        this.message = message;
    }

    public UUID getJournalEntryId() { return journalEntryId; }
    public BigDecimal getOpeningBalanceEquityAmount() { return openingBalanceEquityAmount; }
    public String getMessage() { return message; }
}
