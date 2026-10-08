package com.bradox.erp.accounting.service.domain.ports.input.service;

import com.bradox.erp.accounting.service.domain.ports.output.repository.AccountBalanceRepository;
import com.bradox.erp.accounting.service.domain.report.BalanceSheetReport;
import com.bradox.erp.accounting.service.domain.report.GeneralLedgerLine;
import com.bradox.erp.accounting.service.domain.report.PartnerLedgerReport;
import com.bradox.erp.accounting.service.domain.report.ProfitAndLossReport;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportingApplicationService {

    /**
     * Trial balance: one line per account with balance (sum(debit)-sum(credit)) from posted
     * journal items in the date range.
     */
    default List<AccountBalanceRepository.AccountBalanceLine> getTrialBalance(UUID companyId, LocalDate from, LocalDate to) {
        return getTrialBalance(companyId, from, to, null, null);
    }

    /** Optional analytic dimension filter, e.g. {@code tsh.project} and a project id (labor cost per project). */
    List<AccountBalanceRepository.AccountBalanceLine> getTrialBalance(UUID companyId, LocalDate from, LocalDate to,
                                                                     String analyticModel, UUID analyticId);

    BalanceSheetReport getBalanceSheet(UUID companyId, LocalDate asOf);

    ProfitAndLossReport getProfitAndLoss(UUID companyId, LocalDate from, LocalDate to);

    default List<GeneralLedgerLine> getGeneralLedger(UUID companyId, LocalDate from, LocalDate to, UUID accountId) {
        return getGeneralLedger(companyId, from, to, accountId, null, null);
    }

    List<GeneralLedgerLine> getGeneralLedger(UUID companyId, LocalDate from, LocalDate to, UUID accountId,
                                             String analyticModel, UUID analyticId);

    /**
     * Partner subsidiary ledger: posted journal lines on receivable/payable accounts, grouped by partner.
     * When {@code partnerId} is null, returns summaries for every partner with non-zero opening or period activity.
     * When set, returns that partner's summary plus movement lines with running balance.
     */
    PartnerLedgerReport getPartnerLedger(UUID companyId, LocalDate from, LocalDate to, UUID partnerId);
}
