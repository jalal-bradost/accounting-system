package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.input.service;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.BalanceSheetReport;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.GeneralLedgerLine;
import com.jalaldeveloper.accountingsystem.accounting.service.domain.report.ProfitAndLossReport;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportingApplicationService {

    /**
     * Trial balance: one line per account with balance (sum(debit)-sum(credit)) from posted
     * journal items in the date range.
     */
    List<AccountBalanceRepository.AccountBalanceLine> getTrialBalance(UUID companyId, LocalDate from, LocalDate to);

    BalanceSheetReport getBalanceSheet(UUID companyId, LocalDate asOf);

    ProfitAndLossReport getProfitAndLoss(UUID companyId, LocalDate from, LocalDate to);

    List<GeneralLedgerLine> getGeneralLedger(UUID companyId, LocalDate from, LocalDate to, UUID accountId);
}
