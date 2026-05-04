package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Output port for querying account balances from posted journal items (trial balance / ledger).
 */
public interface AccountBalanceRepository {

    /**
     * Returns one line per account: accountId and balance (sum(debit)-sum(credit)) for posted
     * journal items in the company and within the date range (inclusive). Only accounts with
     * at least one posted line are included.
     */
    List<AccountBalanceLine> getTrialBalance(CompanyId companyId, LocalDate from, LocalDate to);

    /**
     * Cumulative balance per account (sum(debit) − sum(credit)) for posted lines with
     * entry date on or before {@code asOf}, restricted to the given account types.
     */
    List<AccountBalanceLine> getBalancesUpTo(CompanyId companyId, LocalDate asOf, List<AccountType> accountTypes);

    record AccountBalanceLine(UUID accountId, BigDecimal balance) {}
}
