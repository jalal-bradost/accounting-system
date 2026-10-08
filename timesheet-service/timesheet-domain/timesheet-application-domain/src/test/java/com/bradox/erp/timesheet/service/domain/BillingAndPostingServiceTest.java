package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.exception.DomainException;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.AssignLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.dto.ProjectCommand;
import com.bradox.erp.timesheet.service.domain.dto.SetRatesCommand;
import com.bradox.erp.timesheet.service.domain.dto.SettingsCommand;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort.SaleLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TSH-06 billing through sales lines and TSH-10 ledger posting, end to end over fakes. */
class BillingAndPostingServiceTest extends ServiceTestBase {

    private final UUID worker2Id = null;

    @BeforeEach
    void managerWhoMayApprove() {
        permissions.addAll(List.of(TimesheetPermissions.APPROVE_ALL, TimesheetPermissions.BILLING_MANAGE,
                TimesheetPermissions.POSTING_MANAGE, TimesheetPermissions.REOPEN, TimesheetPermissions.ENTRY_ON_BEHALF,
                TimesheetPermissions.PROJECT_MANAGE, TimesheetPermissions.TASK_MANAGE, TimesheetPermissions.COST_VIEW));
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);   // the test user approves their own week
        settings.save(st);
    }

    private Project billable(String name, SaleLine line) {
        Project p = project(name, BillingMode.HOURLY, true);
        p.configureAccounting(line == null ? null : line.lineId(), null);
        return p;
    }

    private WeekSummaryResponse approve(LocalDate day) {
        var w = weekService.submit(COMPANY, new SubmitWeekCommand(null, day));
        return weekService.approve(COMPANY, w.weekId());
    }

    private void enableLedger() {
        var st = access.settings(COMPANY);
        st.updateLedger(true, ledger.costAccount, ledger.appliedAccount, "TSH");
        settings.save(st);
    }

    // ------------------------------------------------------------------ TSH-06

    @Test
    void approvalSetsTheDeliveredQuantityFromApprovedBillableHours() {
        SaleLine line = salesLines.add("S00070", "Consulting", "HOURS", true, false, 0);
        Project p = billable("Acme", line);
        entryService.log(COMPANY, log(TODAY, p, null, 90));
        entryService.log(COMPANY, log(TODAY.minusDays(1), p, null, 30));
        approve(TODAY);
        assertEquals(new BigDecimal("2.0000"), salesLines.lines.get(line.lineId()).qtyDelivered().setScale(4), "120 min = 2.00 h");
        assertEquals(line.lineId(), entries.store.values().iterator().next().getSaleLineId());
    }

    @Test
    void daysUseMinutesPerDay() {
        SaleLine line = salesLines.add("S1", "Day rate", "DAYS", true, false, 0);
        Project p = billable("Days", line);
        entryService.log(COMPANY, log(TODAY, p, null, 480));
        entryService.log(COMPANY, log(TODAY.minusDays(1), p, null, 240));
        approve(TODAY);
        assertEquals(0, new BigDecimal("1.50").compareTo(salesLines.lines.get(line.lineId()).qtyDelivered()));
    }

    @Test
    void taskLineThenEmployeeRateThenProjectDefault() {
        SaleLine def = salesLines.add("S1", "Default", "HOURS", true, false, 0);
        SaleLine rate = salesLines.add("S1", "Senior", "HOURS", true, false, 0);
        SaleLine taskLine = salesLines.add("S1", "Special", "HOURS", true, false, 0);
        Project p = billable("Mixed", def);
        Task t = task(p, "Special work");
        t.assignSaleLine(taskLine.lineId());
        rates.byProject.put(p.getId().getId(), Map.of(me.id(), rate.lineId()));

        entryService.log(COMPANY, log(TODAY, p, t, 60));                       // task line wins
        entryService.log(COMPANY, log(TODAY.minusDays(1), p, null, 120));      // the employee's rate
        approve(TODAY);
        assertEquals(0, BigDecimal.ONE.compareTo(salesLines.lines.get(taskLine.lineId()).qtyDelivered()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(salesLines.lines.get(rate.lineId()).qtyDelivered()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salesLines.lines.get(def.lineId()).qtyDelivered()));
    }

    @Test
    void nonBillableAndFixedPriceHoursNeverChangeASalesQuantity() {
        SaleLine line = salesLines.add("S1", "Line", "HOURS", true, false, 0);
        Project hourly = billable("Hourly", line);
        Project fixed = project("Fixed", BillingMode.FIXED_PRICE, false);
        entryService.log(COMPANY, new EntryCommand(null, TODAY, hourly.getId().getId(), null, 60, null, null, false));
        entryService.log(COMPANY, log(TODAY, fixed, null, 60));
        approve(TODAY);
        assertTrue(salesLines.calls.isEmpty());
    }

    @Test
    void billableEntryWithoutALineIsFlaggedAndOneClickFixesIt() {
        Project p = billable("Orphan", null);
        var logged = entryService.log(COMPANY, log(TODAY, p, null, 120));
        approve(TODAY);
        assertTrue(salesLines.calls.isEmpty(), "no line, no quantity");
        var attention = billingService.needsAttention(COMPANY);
        assertEquals(1, attention.size());
        assertEquals("NO_LINE", attention.get(0).reason());

        SaleLine line = salesLines.add("S9", "Chosen", "HOURS", true, false, 0);
        var fixed = billingService.assignLine(COMPANY, logged.id(), line.lineId());
        assertEquals(line.lineId(), fixed.saleLineId());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(salesLines.lines.get(line.lineId()).qtyDelivered()));
        assertTrue(billingService.needsAttention(COMPANY).isEmpty());
    }

    @Test
    void closedOrdersAreNotUpdatedAndStayOnTheAttentionList() {
        SaleLine line = salesLines.add("S1", "Closed", "HOURS", true, true, 0);
        Project p = billable("Closed", line);
        entryService.log(COMPANY, log(TODAY, p, null, 60));
        approve(TODAY);
        assertTrue(salesLines.calls.isEmpty());
        var attention = billingService.needsAttention(COMPANY);
        assertEquals(1, attention.size());
        assertEquals("ORDER_CLOSED", attention.get(0).reason());
    }

    @Test
    void salesRefusingDoesNotUndoTheApproval() {
        SaleLine line = salesLines.add("S1", "Line", "HOURS", true, false, 0);
        Project p = billable("Refused", line);
        entryService.log(COMPANY, log(TODAY, p, null, 60));
        salesLines.failWith = new com.bradox.erp.accounting.service.domain.AccountingTestException("order is locked");
        assertEquals(WeekStatus.APPROVED, approve(TODAY).status());
    }

    @Test
    void reopeningRemovesTheHoursFromTheQuantity() {
        SaleLine line = salesLines.add("S1", "Line", "HOURS", true, false, 0);
        Project p = billable("Reopen", line);
        entryService.log(COMPANY, log(TODAY, p, null, 120));
        var w = approve(TODAY);
        assertEquals(0, BigDecimal.valueOf(2).compareTo(salesLines.lines.get(line.lineId()).qtyDelivered()));
        weekService.reopen(COMPANY, w.weekId(), new WeekReasonCommand("typo"));
        assertEquals(0, BigDecimal.ZERO.compareTo(salesLines.lines.get(line.lineId()).qtyDelivered()));
        assertNull(entries.store.values().iterator().next().getSaleLineId());
    }

    @Test
    void reopenIsBlockedWhenHoursAreAlreadyInvoiced() {
        SaleLine line = salesLines.add("S1", "Invoiced", "HOURS", true, false, 2);   // 2 h already invoiced
        Project p = billable("Invoiced", line);
        entryService.log(COMPANY, log(TODAY, p, null, 120));
        var w = approve(TODAY);
        TimesheetDomainException ex = assertThrows(TimesheetDomainException.class,
                () -> weekService.reopen(COMPANY, w.weekId(), new WeekReasonCommand("typo")));
        assertEquals("error.timesheet.reopenBlockedInvoiced", ex.getMessageKey());
        assertEquals(WeekStatus.APPROVED, weeks.store.values().iterator().next().getStatus(), "nothing changed");
        assertEquals(0, BigDecimal.valueOf(2).compareTo(salesLines.lines.get(line.lineId()).qtyDelivered()));
    }

    @Test
    void billingStatusAllocatesTheInvoicedQuantityOldestFirst() {
        SaleLine line = salesLines.add("S1", "Fifo", "HOURS", true, false, 1.5);   // 90 of 120 minutes invoiced
        Project p = billable("Fifo", line);
        var first = entryService.log(COMPANY, log(TODAY.minusDays(1), p, null, 60));
        var second = entryService.log(COMPANY, log(TODAY, p, null, 60));
        var draftStatuses = entryService.list(COMPANY, null, false, null, null, null, null, null).items();
        assertTrue(draftStatuses.stream().allMatch(e -> "PENDING_APPROVAL".equals(e.billingStatus())));
        approve(TODAY);
        var items = entryService.list(COMPANY, null, false, null, null, null, null, null).items();
        assertEquals("INVOICED", items.stream().filter(e -> e.id().equals(first.id())).findFirst().orElseThrow().billingStatus());
        assertEquals("PARTIAL", items.stream().filter(e -> e.id().equals(second.id())).findFirst().orElseThrow().billingStatus());
    }

    @Test
    void notBillableEntriesSayso() {
        Project p = project("Free", BillingMode.HOURLY, false);
        entryService.log(COMPANY, log(TODAY, p, null, 60));
        assertEquals("NOT_BILLABLE", entryService.list(COMPANY, null, false, null, null, null, null, null).items().get(0).billingStatus());
    }

    @Test
    void ratesNeedPermissionAndEligibleLinesAndRejectFixedPrice() {
        SaleLine ok = salesLines.add("S1", "Ok", "HOURS", true, false, 0);
        SaleLine nope = salesLines.add("S1", "Not timesheet", "HOURS", false, false, 0);
        Project p = billable("Rates", null);
        var rates1 = billingService.setRates(COMPANY, p.getId().getId(), new SetRatesCommand(List.of(new SetRatesCommand.Rate(me.id(), ok.lineId()))));
        assertEquals(1, rates1.size());
        assertEquals("S1 · Ok", rates1.get(0).saleLineLabel());
        assertThrows(TimesheetDomainException.class, () -> billingService.setRates(COMPANY, p.getId().getId(),
                new SetRatesCommand(List.of(new SetRatesCommand.Rate(me.id(), nope.lineId())))));
        assertThrows(TimesheetDomainException.class, () -> billingService.setRates(COMPANY, p.getId().getId(),
                new SetRatesCommand(List.of(new SetRatesCommand.Rate(me.id(), ok.lineId()), new SetRatesCommand.Rate(me.id(), ok.lineId())))));
        Project fixed = project("Fixed", BillingMode.FIXED_PRICE, false);
        assertThrows(TimesheetDomainException.class, () -> billingService.setRates(COMPANY, fixed.getId().getId(), new SetRatesCommand(List.of())));
        permissions.remove(TimesheetPermissions.BILLING_MANAGE);
        assertThrows(ForbiddenException.class, () -> billingService.setRates(COMPANY, p.getId().getId(), new SetRatesCommand(List.of())));
        assertThrows(ForbiddenException.class, () -> billingService.needsAttention(COMPANY));
        assertThrows(ForbiddenException.class, () -> billingService.assignLine(COMPANY, UUID.randomUUID(), ok.lineId()));
    }

    @Test
    void projectDefaultLineIsValidatedAndNeedsBillingRights() {
        SaleLine ok = salesLines.add("S1", "Ok", "HOURS", true, false, 0);
        SaleLine bad = salesLines.add("S1", "Bad", "HOURS", false, false, 0);
        var created = projectService.create(COMPANY, new ProjectCommand("Billed", null, null, null, true, BillingMode.HOURLY, true, null, null,
                ok.lineId(), null));
        assertEquals("S1 · Ok", created.defaultSaleLineLabel());
        assertThrows(TimesheetDomainException.class, () -> projectService.create(COMPANY, new ProjectCommand("Bad", null, null, null, true,
                BillingMode.HOURLY, true, null, null, bad.lineId(), null)));
        permissions.remove(TimesheetPermissions.BILLING_MANAGE);
        assertThrows(ForbiddenException.class, () -> projectService.update(COMPANY, created.id(), new ProjectCommand("Billed", null, null, null,
                true, BillingMode.HOURLY, true, null, null, null, null)), "removing the line is a billing change");
        // an update that keeps the line is not a billing change
        projectService.update(COMPANY, created.id(), new ProjectCommand("Billed renamed", null, null, null, true, BillingMode.HOURLY, true,
                null, null, ok.lineId(), null));
    }

    @Test
    void taskLineIsValidated() {
        SaleLine ok = salesLines.add("S1", "Ok", "HOURS", true, false, 0);
        Project p = billable("Tasks", null);
        var t = taskService.create(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.TaskCommand(p.getId().getId(), "Install", null,
                null, null, null, ok.lineId()));
        assertEquals(ok.lineId(), t.saleLineId());
        SaleLine bad = salesLines.add("S1", "Bad", "HOURS", false, false, 0);
        assertThrows(TimesheetDomainException.class, () -> taskService.update(COMPANY, t.id(),
                new com.bradox.erp.timesheet.service.domain.dto.TaskCommand(null, "Install", null, null, null, null, bad.lineId())));
    }

    // ------------------------------------------------------------------ TSH-10

    @Test
    void nothingIsPostedWhileLedgerPostingIsOff() {
        costs.perHour = new BigDecimal("1000");
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        approve(TODAY);
        assertTrue(ledger.posted.isEmpty());
        assertTrue(postings.store.isEmpty());
    }

    @Test
    void approvalPostsOneBalancedEntryPerEmployeeWeekWithAProjectDimension() {
        costs.perHour = new BigDecimal("6000");
        enableLedger();
        Project a = project("Alpha", BillingMode.HOURLY, false);
        Project b = project("Beta", BillingMode.HOURLY, false);
        UUID betaAccount = UUID.randomUUID();
        b.configureAccounting(null, betaAccount);
        entryService.log(COMPANY, log(TODAY, a, null, 90));
        entryService.log(COMPANY, log(TODAY.minusDays(1), b, null, 30));
        var w = approve(TODAY);

        assertEquals(1, ledger.posted.size(), "one entry for the employee-week");
        var req = ledger.posted.get(0);
        assertEquals("TSH", req.journalCode());
        assertEquals(ledger.appliedAccount, req.creditAccountId());
        assertEquals(LocalDate.of(2026, 10, 9), req.entryDate(), "dated the last day of the work week");
        assertEquals(2, req.debits().size());
        BigDecimal debits = req.debits().stream().map(d -> d.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("12000").compareTo(debits));
        var alpha = req.debits().stream().filter(d -> d.projectId().equals(a.getId().getId())).findFirst().orElseThrow();
        var beta = req.debits().stream().filter(d -> d.projectId().equals(b.getId().getId())).findFirst().orElseThrow();
        assertEquals(ledger.costAccount, alpha.accountId(), "company default cost account");
        assertEquals(betaAccount, beta.accountId(), "the project's own cost account");
        assertEquals(0, new BigDecimal("9000").compareTo(alpha.amount()));

        WeekPosting posting = postings.findActiveByWeek(weeks.store.values().iterator().next().getId()).orElseThrow();
        assertEquals(PostingStatus.POSTED, posting.getStatus());
        assertEquals(1, posting.getVersion());
        assertNotNull(posting.getJournalEntryId());
        assertFalse(posting.isLatePosted());
        assertEquals(PostingStatus.POSTED, weekService.get(COMPANY, w.weekId()).postingStatus());
    }

    @Test
    void closedPeriodFallsBackToTheApprovalDateAndIsFlaggedLate() {
        costs.perHour = new BigDecimal("1000");
        enableLedger();
        ledger.periodClosedForWeekEnd = true;
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        var w = approve(TODAY);
        var posting = postings.store.values().iterator().next();
        assertTrue(posting.isLatePosted());
        assertEquals(TODAY, posting.getEntryDate());
        assertTrue(weekService.get(COMPANY, w.weekId()).latePosted());
    }

    @Test
    void zeroCostWeekIsSkippedNotPosted() {
        enableLedger();          // no cost rate, so every entry is flagged missing
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        approve(TODAY);
        assertTrue(ledger.posted.isEmpty());
        assertEquals(PostingStatus.SKIPPED, postings.store.values().iterator().next().getStatus());
    }

    @Test
    void accountingFailureDoesNotUndoApprovalAndIsRetriedUpToFiveTimes() {
        costs.perHour = new BigDecimal("1000");
        enableLedger();
        ledger.failWith = new com.bradox.erp.accounting.service.domain.AccountingTestException("journal TSH not configured");
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        var w = approve(TODAY);
        assertEquals(WeekStatus.APPROVED, w.status());
        WeekPosting posting = postings.store.values().iterator().next();
        assertEquals(PostingStatus.FAILED, posting.getStatus());
        assertEquals(1, posting.getAttempts());
        assertTrue(posting.getErrorMessage().contains("journal TSH"));
        assertEquals(PostingStatus.FAILED, weekService.get(COMPANY, w.weekId()).postingStatus());

        for (int i = 0; i < 6; i++) {
            jobs.retryPostings();
        }
        assertEquals(WeekPosting.MAX_ATTEMPTS, posting.getAttempts(), "the job gives up after five attempts");
        assertEquals(PostingStatus.FAILED, posting.getStatus());

        ledger.failWith = null;
        jobs.retryPostings();
        assertEquals(PostingStatus.FAILED, posting.getStatus(), "no more automatic attempts");
        var retried = postingService.retry(COMPANY, posting.getId().getId());
        assertEquals(PostingStatus.POSTED, retried.status());
        assertEquals(1, ledger.posted.size());
    }

    @Test
    void retryingTwiceNeverPostsTwice() {
        costs.perHour = new BigDecimal("1000");
        enableLedger();
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        approve(TODAY);
        UUID id = postings.store.values().iterator().next().getId().getId();
        postingCoordinator.processNow(id);
        postingCoordinator.processNow(id);
        jobs.retryPostings();
        assertEquals(1, ledger.posted.size());
        assertThrows(TimesheetDomainException.class, () -> postingService.retry(COMPANY, id), "a posted entry is not retried");
    }

    @Test
    void missingAccountsFailTheRowWithAClearMessage() {
        costs.perHour = new BigDecimal("1000");
        var st = access.settings(COMPANY);
        st.updateLedger(true, null, null, "TSH");
        settings.save(st);
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        approve(TODAY);
        var posting = postings.store.values().iterator().next();
        assertEquals(PostingStatus.FAILED, posting.getStatus());
        assertTrue(posting.getErrorMessage().toLowerCase().contains("account"));
    }

    @Test
    void reopenReversesThePostingAndReapprovalCreatesVersionTwo() {
        costs.perHour = new BigDecimal("1000");
        enableLedger();
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        var w = approve(TODAY);
        UUID firstEntry = postings.store.values().iterator().next().getJournalEntryId();

        weekService.reopen(COMPANY, w.weekId(), new WeekReasonCommand("wrong hours"));
        assertEquals(List.of(firstEntry), ledger.reversed);
        var first = postings.store.values().iterator().next();
        assertEquals(PostingStatus.REVERSED, first.getStatus());
        assertNotNull(first.getReversalEntryId());

        weekService.approve(COMPANY, submitAgain(w));
        assertEquals(2, postings.store.size());
        long active = postings.store.values().stream().filter(WeekPosting::isActive).count();
        assertEquals(1, active, "a week never has two active postings");
        assertEquals(2, postings.findActiveByWeek(first.getWeekId()).orElseThrow().getVersion());
        assertEquals(2, ledger.posted.size());
    }

    private UUID submitAgain(WeekSummaryResponse w) {
        return weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY)).weekId();
    }

    @Test
    void reopenIsRefusedWhenTheReversalFails() {
        costs.perHour = new BigDecimal("1000");
        enableLedger();
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        var w = approve(TODAY);
        ledger.failWith = new com.bradox.erp.accounting.service.domain.AccountingTestException("period is closed");
        TimesheetDomainException ex = assertThrows(TimesheetDomainException.class,
                () -> weekService.reopen(COMPANY, w.weekId(), new WeekReasonCommand("oops")));
        assertEquals("error.timesheet.reversalFailed", ex.getMessageKey());
        assertTrue(ex.getMessage().contains("period is closed"));
        assertEquals(PostingStatus.POSTED, postings.store.values().iterator().next().getStatus());
    }

    @Test
    void foreignCurrencyCostIsConvertedToTheBaseCurrency() {
        costs.perHour = new BigDecimal("10");
        costs.currency = "USD";
        ledger.rate = new BigDecimal("1500");
        enableLedger();
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        approve(TODAY);
        assertEquals(0, new BigDecimal("15000").compareTo(ledger.posted.get(0).debits().get(0).amount()));
    }

    @Test
    void backfillPostsEarlierWeeksOldestFirstWithAResultPerWeek() {
        costs.perHour = new BigDecimal("1000");
        Project p = project("Old", BillingMode.HOURLY, false);
        entryService.log(COMPANY, log(TODAY.minusDays(14), p, null, 60));
        entryService.log(COMPANY, log(TODAY.minusDays(7), p, null, 120));
        approve(TODAY.minusDays(7));
        approve(TODAY.minusDays(14));
        assertTrue(ledger.posted.isEmpty(), "posting was off when they were approved");

        enableLedger();
        var result = postingService.backfill(COMPANY);
        assertEquals(2, result.posted());
        assertEquals(0, result.failed());
        assertTrue(result.items().get(0).weekStart().isBefore(result.items().get(1).weekStart()), "oldest first");
        assertEquals(2, ledger.posted.size());
        assertEquals(0, postingService.backfill(COMPANY).items().size(), "a second run finds nothing new");
    }

    @Test
    void postingScreensNeedThePermission() {
        permissions.remove(TimesheetPermissions.POSTING_MANAGE);
        assertThrows(ForbiddenException.class, () -> postingService.list(COMPANY, null));
        assertThrows(ForbiddenException.class, () -> postingService.retry(COMPANY, UUID.randomUUID()));
        assertThrows(ForbiddenException.class, () -> postingService.backfill(COMPANY));
        entryService.log(COMPANY, log(TODAY, project("P", BillingMode.HOURLY, false), null, 60));
        var w = approve(TODAY);
        assertNull(weekService.get(COMPANY, w.weekId()).postingStatus(), "posting state is hidden without the permission");
    }

    @Test
    void enablingPostingProvisionsDefaultAccounts() {
        permissions.add(TimesheetPermissions.SETTINGS_MANAGE);
        var s = settingsService.update(COMPANY, new SettingsCommand(null, null, null, null, null, null, null, null, null, null, null,
                true, null, null, "tsh"));
        assertTrue(s.ledgerPostingEnabled());
        assertEquals(ledger.costAccount, s.defaultCostAccountId());
        assertEquals(ledger.appliedAccount, s.laborAppliedAccountId());
        assertEquals("TSH", s.journalCode());
        permissions.remove(TimesheetPermissions.POSTING_MANAGE);
        assertThrows(ForbiddenException.class, () -> settingsService.update(COMPANY, new SettingsCommand(null, null, null, null, null, null,
                null, null, null, null, null, false, null, null, null)));
    }
}
