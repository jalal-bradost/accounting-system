package com.bradox.delin.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One posted journal line in the general ledger with running balance (debit − credit)
 * for its account within the requested range.
 */
public record GeneralLedgerLine(
        UUID accountId,
        UUID journalEntryId,
        LocalDateTime entryDate,
        String journalCode,
        String sequenceNumber,
        String label,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance
) {}
