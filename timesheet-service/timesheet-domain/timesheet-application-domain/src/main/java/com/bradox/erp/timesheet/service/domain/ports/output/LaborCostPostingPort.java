package com.bradox.erp.timesheet.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Posts labor cost to the general ledger and reverses it (TSH-10). Implemented in {@code infrastructure} over Accounting. */
public interface LaborCostPostingPort {

    /** One debit per project; {@code projectId} becomes the analytic dimension {@code tsh.project}. */
    record DebitLine(UUID accountId, UUID projectId, BigDecimal amount, String label) {
    }

    /**
     * {@code entryDate} is tried first; when its fiscal period is closed the entry is dated {@code fallbackDate} and
     * flagged late (TSH-10 #3).
     */
    record Request(CompanyId companyId, String journalCode, UUID creditAccountId, String creditLabel, LocalDate entryDate,
                   LocalDate fallbackDate, String reference, List<DebitLine> debits) {
    }

    record Result(UUID journalEntryId, LocalDate entryDate, boolean late) {
    }

    /** Creates and posts one balanced journal entry. Throws a domain exception with a readable message on failure. */
    Result post(Request request);

    /** Posts a reversal of an earlier entry and returns the reversal entry id. */
    UUID reverse(CompanyId companyId, UUID journalEntryId, String reason);

    record Defaults(UUID costAccountId, UUID laborAppliedAccountId) {
    }

    /** Makes sure the journal and the two labor accounts exist, creating them if absent (TSH-10, accountant confirms mapping). */
    Defaults ensureDefaults(CompanyId companyId, String journalCode);

    /** Posted movement (debit minus credit) on the payroll "Salary Expense" account in the period, for the variance tile. */
    BigDecimal salaryExpense(CompanyId companyId, LocalDate from, LocalDate to);

    String baseCurrency(CompanyId companyId);

    BigDecimal toBaseCurrency(CompanyId companyId, BigDecimal amount, String currencyCode, LocalDate asOf);
}
