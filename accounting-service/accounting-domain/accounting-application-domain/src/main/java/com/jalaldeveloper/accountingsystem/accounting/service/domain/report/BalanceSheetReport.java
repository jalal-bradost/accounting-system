package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Balance sheet as of a date: permanent accounts (asset, liability, equity) with
 * cumulative posted activity through {@code asOf}. Amounts use statement presentation
 * (assets debit-positive; liabilities and equity credit-positive).
 */
public record BalanceSheetReport(
        LocalDate asOf,
        List<AccountLine> assets,
        List<AccountLine> liabilities,
        List<AccountLine> equity,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquity
) {
    public record AccountLine(UUID accountId, BigDecimal amount, String name) {
        public AccountLine(UUID accountId, BigDecimal amount) {
            this(accountId, amount, null);
        }
    }
}
