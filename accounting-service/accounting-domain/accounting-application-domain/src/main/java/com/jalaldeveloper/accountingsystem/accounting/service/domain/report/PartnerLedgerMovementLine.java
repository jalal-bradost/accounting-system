package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One posted journal line on a trade account for a partner, with running balance in company currency. */
public record PartnerLedgerMovementLine(
        LocalDate entryDate,
        UUID journalEntryId,
        String journalCode,
        String sequenceNumber,
        String accountCode,
        String accountName,
        String label,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance,
        UUID reconciliationId) {}
