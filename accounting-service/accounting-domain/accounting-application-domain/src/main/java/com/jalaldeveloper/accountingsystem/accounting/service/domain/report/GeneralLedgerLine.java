package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One posted journal line in the general ledger with running balance (debit − credit)
 * for its account within the requested range.
 */
public record GeneralLedgerLine(
        UUID accountId,
        UUID journalEntryId,
        LocalDate entryDate,
        String journalCode,
        String sequenceNumber,
        String label,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance
) {}
