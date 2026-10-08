package com.bradox.erp.integration;

import com.bradox.erp.accounting.service.domain.JournalEntryTiming;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.create.CreateJournalEntryResponse;
import com.bradox.erp.accounting.service.domain.create.JournalItemCommand;
import com.bradox.erp.accounting.service.domain.create.ReverseJournalEntryCommand;
import com.bradox.erp.accounting.service.domain.ports.input.service.JournalEntryApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.CurrencyConversionPort;
import com.bradox.erp.dataaccess.entity.AccountEntity;
import com.bradox.erp.dataaccess.entity.JournalEntity;
import com.bradox.erp.dataaccess.repository.AccountJpaRepository;
import com.bradox.erp.dataaccess.repository.JournalJpaRepository;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalType;
import com.bradox.erp.domain.exception.DomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Posts labor cost for Timesheet (TSH-10) through the accounting application service: one balanced journal entry per
 * employee-week in journal {@code TSH}, a debit per project carrying the project as analytic dimension, and one credit to
 * "Labor cost applied". Each attempt runs in its own transaction, so a refused date (closed period) cannot poison the
 * caller's transaction and the late fallback date gets a clean try.
 */
@Component
public class TimesheetLaborCostPostingAdapter implements LaborCostPostingPort {

    static final String ANALYTIC_MODEL = "tsh.project";
    static final String COST_ACCOUNT_CODE = "430030";
    static final String APPLIED_ACCOUNT_CODE = "430031";
    /** The payroll "Salary Expense" account the labor cost is compared with. */
    static final String SALARY_ACCOUNT_CODE = "430016";

    private final JournalEntryApplicationService journalService;
    private final JournalJpaRepository journals;
    private final AccountJpaRepository accounts;
    private final CurrencyConversionPort currency;
    private final ObjectProvider<PlatformTransactionManager> txManager;
    private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    public TimesheetLaborCostPostingAdapter(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc, JournalEntryApplicationService journalService, JournalJpaRepository journals,
                                            AccountJpaRepository accounts, CurrencyConversionPort currency,
                                            ObjectProvider<PlatformTransactionManager> txManager) {
        this.journalService = journalService;
        this.journals = journals;
        this.accounts = accounts;
        this.currency = currency;
        this.txManager = txManager;
        this.jdbc = jdbc;
    }

    @Override
    public Result post(Request r) {
        JournalEntity journal = journals.findByCompanyIdAndCode(r.companyId().getId(), r.journalCode()).orElseThrow(() ->
                new TimesheetDomainException("error.timesheet.journalMissing", new Object[]{r.journalCode()},
                        "Journal '" + r.journalCode() + "' does not exist. Turn on ledger posting in the timesheet settings to create it."));
        String base = currency.baseCurrencyCode(r.companyId().getId());
        BigDecimal total = r.debits().stream().map(DebitLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<JournalItemCommand> items = new ArrayList<>();
        for (DebitLine d : r.debits()) {
            items.add(new JournalItemCommand(d.accountId(), d.label(), d.amount(), BigDecimal.ZERO, base, null, null,
                    ANALYTIC_MODEL, d.projectId()));
        }
        items.add(new JournalItemCommand(r.creditAccountId(), r.creditLabel(), BigDecimal.ZERO, total, base, null, null, null, null));
        try {
            return new Result(attempt(r, journal.getId(), base, items, r.entryDate()), r.entryDate(), false);
        } catch (DomainException ex) {
            // TSH-10 #3: a closed fiscal period (or lock date) moves the entry to the first open date and flags it late.
            if (ex.getMessage() == null || !ex.getMessage().startsWith("Cannot post:") || r.fallbackDate().equals(r.entryDate())) {
                throw ex;
            }
            return new Result(attempt(r, journal.getId(), base, items, r.fallbackDate()), r.fallbackDate(), true);
        }
    }

    private UUID attempt(Request r, UUID journalId, String base, List<JournalItemCommand> items, LocalDate date) {
        return inNewTransaction(() -> {
            CreateJournalEntryResponse created = journalService.createJournalEntry(new CreateJournalEntryCommand(
                    r.companyId().getId(), journalId, r.reference(), JournalEntryTiming.ofBusinessDate(date), base, null, items));
            journalService.postJournalEntry(created.getJournalEntryId());
            return created.getJournalEntryId();
        });
    }

    @Override
    public UUID reverse(CompanyId companyId, UUID journalEntryId, String reason) {
        return inNewTransaction(() -> journalService
                .reverseJournalEntry(new ReverseJournalEntryCommand(journalEntryId, reason)).getReversalJournalEntryId());
    }

    @Override
    public Defaults ensureDefaults(CompanyId companyId, String journalCode) {
        inNewTransaction(() -> {
            if (journals.findByCompanyIdAndCode(companyId.getId(), journalCode).isEmpty()) {
                journals.save(JournalEntity.builder().id(UUID.randomUUID()).companyId(companyId.getId()).code(journalCode)
                        .name("Timesheet labor cost").type(JournalType.MISC).build());
            }
            return null;
        });
        UUID cost = ensureAccount(companyId, COST_ACCOUNT_CODE, "Cost of services – labor", AccountType.COST_OF_REVENUE);
        UUID applied = ensureAccount(companyId, APPLIED_ACCOUNT_CODE, "Labor cost applied", AccountType.EXPENSES);
        return new Defaults(cost, applied);
    }

    private UUID ensureAccount(CompanyId companyId, String code, String name, AccountType type) {
        return inNewTransaction(() -> accounts.findByCompanyIdAndCode(companyId.getId(), code).map(AccountEntity::getId)
                .orElseGet(() -> accounts.save(AccountEntity.builder().id(UUID.randomUUID()).companyId(companyId.getId())
                        .code(code).name(name).type(type).active(true).build()).getId()));
    }

    @Override
    public BigDecimal salaryExpense(CompanyId companyId, LocalDate from, LocalDate to) {
        BigDecimal v = jdbc.queryForObject("select coalesce(sum(i.debit - i.credit), 0) from journal_items i "
                + "join journal_entries e on e.id = i.journal_entry_id join accounts a on a.id = i.account_id "
                + "where e.company_id = :c and e.status = 'POSTED' and a.code = :code and e.entry_date >= :from and e.entry_date <= :to",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("c", companyId.getId()).addValue("code", SALARY_ACCOUNT_CODE)
                        .addValue("from", java.sql.Date.valueOf(from)).addValue("to", java.sql.Date.valueOf(to)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public String baseCurrency(CompanyId companyId) {
        return currency.baseCurrencyCode(companyId.getId());
    }

    @Override
    public BigDecimal toBaseCurrency(CompanyId companyId, BigDecimal amount, String currencyCode, LocalDate asOf) {
        return currency.convertToCompany(companyId.getId(), amount, currencyCode, asOf);
    }

    private <T> T inNewTransaction(Supplier<T> work) {
        PlatformTransactionManager tm = txManager.getIfAvailable();
        if (tm == null) {
            return work.get();
        }
        TransactionTemplate t = new TransactionTemplate(tm);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t.execute(status -> work.get());
    }
}
