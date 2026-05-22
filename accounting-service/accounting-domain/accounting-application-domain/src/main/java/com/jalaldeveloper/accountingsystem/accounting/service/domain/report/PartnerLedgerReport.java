package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Partner subsidiary ledger from posted journal items on trade accounts (receivable / payable).
 * When {@code partnerId} is null, only {@link #summaries()} is populated (all partners with activity).
 * When set, {@link #lines()} contains dated movements with running balance for that partner.
 */
public record PartnerLedgerReport(
        LocalDate fromDate,
        LocalDate toDate,
        UUID partnerId,
        boolean unpostedDraftJournalEntriesThroughPeriodEnd,
        List<PartnerLedgerSummaryRow> summaries,
        List<PartnerLedgerMovementLine> lines) {}
