package com.bradox.delin.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One posted journal line on a trade account for a partner, with running balance in company currency. */
public record PartnerLedgerMovementLine(
        LocalDateTime entryDate,
        UUID journalEntryId,
        String journalCode,
        String journalName,
        String sequenceNumber,
        String accountCode,
        String accountName,
        String label,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance,
        UUID reconciliationId) {}
