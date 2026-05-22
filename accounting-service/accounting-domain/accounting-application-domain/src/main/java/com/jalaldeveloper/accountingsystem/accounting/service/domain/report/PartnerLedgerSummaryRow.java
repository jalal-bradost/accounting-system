package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.math.BigDecimal;
import java.util.UUID;

/** Per-partner totals on receivable/payable accounts: opening (before period), period debits/credits, closing. */
public record PartnerLedgerSummaryRow(
        UUID partnerId,
        String partnerDisplayName,
        BigDecimal openingBalance,
        BigDecimal periodDebit,
        BigDecimal periodCredit,
        BigDecimal closingBalance) {}
