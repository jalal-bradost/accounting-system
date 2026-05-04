package com.jalaldeveloper.accountingsystem.accounting.service.domain.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Income statement for a period: income and expense accounts with statement presentation
 * (revenue credit-positive; expenses debit-positive). Net income is revenue minus expenses.
 */
public record ProfitAndLossReport(
        LocalDate from,
        LocalDate to,
        List<AccountLine> revenue,
        List<AccountLine> expenses,
        BigDecimal totalRevenue,
        BigDecimal totalExpenses,
        BigDecimal netIncome
) {
    public record AccountLine(UUID accountId, BigDecimal amount) {}
}
