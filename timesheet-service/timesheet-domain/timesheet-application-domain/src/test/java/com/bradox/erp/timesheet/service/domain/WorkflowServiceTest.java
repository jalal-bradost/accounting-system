package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.StartTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.StopTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.WeekApplicationService.Scope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Submit/approve (TSH-05), timer (TSH-04), cost (TSH-07) and record targets (TSH-09). */
class WorkflowServiceTest extends ServiceTestBase {

    private WeekSummaryResponse submitMyWeek() {
        return weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
    }

    private Project hourly() {
        return project("Website", BillingMode.HOURLY, true);
    }

    // ---- TSH-05

    @Test
    void emptyWeekCannotBeSubmitted() {
        assertEquals("error.timesheet.emptyWeek", assertThrows(TimesheetDomainException.class, this::submitMyWeek).getMessageKey());
    }

    @Test
    void submitLocksTheWeekForEditing() {
        Project p = hourly();
        entryService.log(COMPANY, log(TODAY, p, null, 120));
        WeekSummaryResponse s = submitMyWeek();
        assertEquals(WeekStatus.SUBMITTED, s.status());
        assertEquals(120, s.totalMinutes());
        assertEquals(LocalDate.of(2026, 10, 3), s.weekStart());
        assertEquals("error.timesheet.weekReadOnly", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, null, 30))).getMessageKey());
        assertEquals("error.timesheet.weekNotSubmittable", assertThrows(TimesheetDomainException.class,
                this::submitMyWeek).getMessageKey());
    }

    @Test
    void weekBelowExpectedHoursStillSubmits() {
        entryService.log(COMPANY, log(TODAY, hourly(), null, 30));
        WeekSummaryResponse s = submitMyWeek();
        assertTrue(s.expectedMinutes() > s.totalMinutes());
        assertEquals(0, s.overtimeMinutes());
    }

    @Test
    void employeesWithoutAScheduleHaveNoOvertime() {
        expected.noSchedule = true;
        entryService.log(COMPANY, log(TODAY, hourly(), null, 600));
        WeekSummaryResponse s = submitMyWeek();
        assertEquals(0, s.expectedMinutes());
        assertEquals(0, s.overtimeMinutes(), "nothing was expected, so nothing is overtime");
    }

    @Test
    void managerApprovesATeamMembersWeekAndCostIsSnapshotted() {
        costs.perHour = new BigDecimal("15000");
        Project p = hourly();
        var boss = employees.add("Boss", null);
        var worker = employees.add("Worker", userId, boss.id(), null);   // the caller is the worker...
        employees.byUser.put(userId, worker.id());
        entryService.log(COMPANY, log(TODAY, p, null, 90));
        WeekSummaryResponse submitted = submitMyWeek();

        // ...then the boss acts. The fake context has one user, so re-point it at the boss.
        employees.byUser.put(userId, boss.id());
        permissions.add(TimesheetPermissions.APPROVE);
        permissions.add(TimesheetPermissions.ENTRY_VIEW_TEAM);
        assertTrue(weekService.list(COMPANY, Scope.APPROVE, null, null, null).stream()
                .anyMatch(w -> w.weekId().equals(submitted.weekId()) && w.canApprove()));
        WeekSummaryResponse approved = weekService.approve(COMPANY, submitted.weekId());
        assertEquals(WeekStatus.APPROVED, approved.status());
        assertEquals("tester", approved.approvedBy());

        var entry = entries.store.values().iterator().next();
        assertEquals(new BigDecimal("15000"), entry.getCostRate().stripTrailingZeros().setScale(0));
        assertEquals(new BigDecimal("22500.0000"), entry.getCostAmount());
        assertFalse(entry.isCostMissing());
    }

    @Test
    void strangerCannotApproveAndNobodyApprovesOwnWeek() {
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        WeekSummaryResponse s = submitMyWeek();
        permissions.add(TimesheetPermissions.APPROVE);
        assertThrows(ForbiddenException.class, () -> weekService.approve(COMPANY, s.weekId()), "own week, team approver");
        permissions.add(TimesheetPermissions.APPROVE_ALL);
        assertThrows(ForbiddenException.class, () -> weekService.approve(COMPANY, s.weekId()),
                "a manager needs 'managers may self-approve'");
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        assertEquals(WeekStatus.APPROVED, weekService.approve(COMPANY, s.weekId()).status());
    }

    @Test
    void approvedWeekWithoutRateFlagsEntriesAndRecomputeFixesOnlyThose() {
        Project p = hourly();
        entryService.log(COMPANY, log(TODAY, p, null, 60));
        WeekSummaryResponse s = submitMyWeek();
        permissions.addAll(List.of(TimesheetPermissions.APPROVE_ALL, TimesheetPermissions.COST_VIEW));
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        weekService.approve(COMPANY, s.weekId());
        var e = entries.store.values().iterator().next();
        assertTrue(e.isCostMissing());
        assertEquals(0, BigDecimal.ZERO.compareTo(e.getCostAmount()));

        assertEquals(0, costService.recomputeMissing(COMPANY).recomputed(), "still no rate");
        costs.perHour = new BigDecimal("6000");
        var result = costService.recomputeMissing(COMPANY);
        assertEquals(1, result.recomputed());
        assertFalse(e.isCostMissing());
        assertEquals(new BigDecimal("6000.0000"), e.getCostAmount());
        // a second recompute never touches a priced entry, even if the rate changed (D6)
        costs.perHour = new BigDecimal("9999");
        assertEquals(0, costService.recomputeMissing(COMPANY).recomputed());
        assertEquals(new BigDecimal("6000.0000"), e.getCostAmount());
    }

    @Test
    void refuseSendsTheWeekBackWithAReason() {
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        WeekSummaryResponse s = submitMyWeek();
        permissions.add(TimesheetPermissions.APPROVE_ALL);
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        assertThrows(Exception.class, () -> weekService.refuse(COMPANY, s.weekId(), new WeekReasonCommand(" ")));
        WeekSummaryResponse refused = weekService.refuse(COMPANY, s.weekId(), new WeekReasonCommand("Tuesday is missing"));
        assertEquals(WeekStatus.REFUSED, refused.status());
        assertEquals("Tuesday is missing", refused.refusedReason());
        // the employee can fix and resubmit
        entryService.log(COMPANY, log(TODAY.minusDays(1), hourly(), null, 60));
        assertEquals(WeekStatus.SUBMITTED, submitMyWeek().status());
    }

    @Test
    void reopenNeedsPermissionAndClearsTheCostSnapshot() {
        costs.perHour = new BigDecimal("1000");
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        WeekSummaryResponse s = submitMyWeek();
        permissions.add(TimesheetPermissions.APPROVE_ALL);
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        weekService.approve(COMPANY, s.weekId());
        assertThrows(ForbiddenException.class, () -> weekService.reopen(COMPANY, s.weekId(), new WeekReasonCommand("typo")));
        permissions.add(TimesheetPermissions.REOPEN);
        WeekSummaryResponse reopened = weekService.reopen(COMPANY, s.weekId(), new WeekReasonCommand("typo"));
        assertEquals(WeekStatus.DRAFT, reopened.status());
        assertNull(entries.store.values().iterator().next().getCostAmount(), "re-approval will price it again");
        assertTrue(gridService.grid(COMPANY, null, TODAY).editable());
    }

    @Test
    void bulkApproveReportsEachWeek() {
        var a = employees.add("A", null);
        var b = employees.add("B", null);
        permissions.addAll(List.of(TimesheetPermissions.ENTRY_ON_BEHALF, TimesheetPermissions.APPROVE_ALL));
        Project p = hourly();
        entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(a.id(), TODAY,
                p.getId().getId(), null, 60, null, null, null));
        entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(b.id(), TODAY,
                p.getId().getId(), null, 60, null, null, null));
        var wa = weekService.submit(COMPANY, new SubmitWeekCommand(a.id(), TODAY));
        var wb = weekService.submit(COMPANY, new SubmitWeekCommand(b.id(), TODAY));
        weekService.approve(COMPANY, wb.weekId());        // already approved: second approval must fail
        var result = weekService.bulkApprove(COMPANY, new BulkApproveCommand(List.of(wa.weekId(), wb.weekId(), UUID.randomUUID())));
        assertEquals(1, result.approved());
        assertEquals(2, result.failed());
        assertTrue(result.items().get(0).ok());
        assertFalse(result.items().get(1).ok());
    }

    @Test
    void approvalOffApprovesOnSubmit() {
        costs.perHour = new BigDecimal("1000");
        var st = access.settings(COMPANY);
        st.update(null, null, null, false, 0, 480);
        settings.save(st);
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        WeekSummaryResponse s = submitMyWeek();
        assertEquals(WeekStatus.APPROVED, s.status());
        assertEquals("system", s.approvedBy());
        assertNotNull(entries.store.values().iterator().next().getCostAmount());
    }

    @Test
    void autoLockLocksOldWeeksAndIsIdempotent() {
        entryService.log(COMPANY, log(TODAY.minusDays(30), hourly(), null, 60));
        var st = access.settings(COMPANY);
        st.updateWorkflow(false, 14, 0, RoundingMode.NEAREST);
        settings.save(st);
        jobs.autoLock();
        var week = weeks.store.values().iterator().next();
        assertTrue(week.isLocked());
        jobs.autoLock();
        assertTrue(week.isLocked());
        assertFalse(gridService.grid(COMPANY, null, TODAY.minusDays(30)).editable());
        // this week is far from the cutoff and stays open
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        jobs.autoLock();
        assertTrue(gridService.grid(COMPANY, null, TODAY).editable());
    }

    @Test
    void teamGridShowsTheTeamOnlyWithPermission() {
        var boss = me;
        var report = employees.add("Report", null, boss.id(), null);
        var outsider = employees.add("Outsider", null);
        permissions.addAll(List.of(TimesheetPermissions.ENTRY_ON_BEHALF));
        Project p = hourly();
        for (var e : List.of(report, outsider)) {
            entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(e.id(), TODAY,
                    p.getId().getId(), null, 90, null, null, null));
        }
        assertThrows(ForbiddenException.class, () -> gridService.teamGrid(COMPANY, TODAY, false));
        permissions.add(TimesheetPermissions.ENTRY_VIEW_TEAM);
        var team = gridService.teamGrid(COMPANY, TODAY, false);
        assertEquals(1, team.employees().size());
        assertEquals("Report", team.employees().get(0).employeeName());
        assertEquals(90, team.employees().get(0).totalMinutes());
        assertEquals(1, team.employees().get(0).rows().size());
        assertThrows(ForbiddenException.class, () -> gridService.teamGrid(COMPANY, TODAY, true));
        permissions.add(TimesheetPermissions.ENTRY_VIEW_ALL);
        assertEquals(3, gridService.teamGrid(COMPANY, TODAY, true).employees().size());
        // a team approver may open a team member's grid, but not an outsider's
        permissions.remove(TimesheetPermissions.ENTRY_VIEW_ALL);
        assertEquals("Report", gridService.grid(COMPANY, report.id(), TODAY).employeeName());
        assertThrows(ForbiddenException.class, () -> gridService.grid(COMPANY, outsider.id(), TODAY));
    }

    // ---- TSH-04

    @Test
    void timerStartsStopsAndCreatesATimerEntry() {
        Project p = hourly();
        var started = timerService.start(COMPANY, new StartTimerCommand(p.getId().getId(), null, "Fixing the sink"));
        assertEquals("Website", started.projectName());
        assertEquals("error.timesheet.timerRunning", assertThrows(TimesheetDomainException.class,
                () -> timerService.start(COMPANY, new StartTimerCommand(p.getId().getId(), null, null))).getMessageKey());
        clock.advanceMinutes(95);
        assertEquals(5700, timerService.current(COMPANY).elapsedSeconds());
        List<EntryResponse> created = timerService.stop(COMPANY, new StopTimerCommand(null, null));
        assertEquals(1, created.size());
        assertEquals(95, created.get(0).minutes());
        assertEquals(EntrySource.TIMER, created.get(0).source());
        assertEquals("Fixing the sink", created.get(0).description());
        assertNull(timerService.current(COMPANY));
        assertEquals("error.timesheet.noTimer", assertThrows(TimesheetDomainException.class,
                () -> timerService.stop(COMPANY, null)).getMessageKey());
    }

    @Test
    void stoppingAppliesTheCompanyRounding() {
        var st = access.settings(COMPANY);
        st.updateWorkflow(false, null, 15, RoundingMode.UP);
        settings.save(st);
        timerService.start(COMPANY, new StartTimerCommand(hourly().getId().getId(), null, null));
        clock.advanceMinutes(16);
        assertEquals(30, timerService.stop(COMPANY, null).get(0).minutes());
    }

    @Test
    void timerOverTwelveHoursNeedsConfirmationAndMidnightSplitsIt() {
        Project p = hourly();
        timerService.start(COMPANY, new StartTimerCommand(p.getId().getId(), null, null));
        clock.advanceMinutes(13 * 60);   // 09:00Z to 22:00Z on the same UTC day; Baghdad is 12:00 to 01:00 next day
        assertEquals("error.timesheet.timerLong", assertThrows(TimesheetDomainException.class,
                () -> timerService.stop(COMPANY, new StopTimerCommand(null, false))).getMessageKey());
        assertNotNull(timerService.current(COMPANY), "the timer survives the refusal");
        // 09:00Z to 22:00Z is 12:00 to 01:00 in Baghdad: 12 h on Oct 8 and 1 h on Oct 9, which is "today" by now.
        List<EntryResponse> created = timerService.stop(COMPANY, new StopTimerCommand(null, true));
        assertEquals(2, created.size());
        assertEquals(LocalDate.of(2026, 10, 8), created.get(0).workDate());
        assertEquals(720, created.get(0).minutes());
        assertEquals(LocalDate.of(2026, 10, 9), created.get(1).workDate());
        assertEquals(60, created.get(1).minutes());
        assertNull(timerService.current(COMPANY));
    }

    @Test
    void timerOnALockedWeekKeepsRunning() {
        Project p = hourly();
        timerService.start(COMPANY, new StartTimerCommand(p.getId().getId(), null, null));
        clock.advanceMinutes(30);
        entryService.log(COMPANY, log(TODAY.minusDays(1), p, null, 60));   // creates this week's row
        weeks.store.values().forEach(w -> w.lock());
        assertThrows(TimesheetDomainException.class, () -> timerService.stop(COMPANY, null));
        assertNotNull(timerService.current(COMPANY));
        timerService.discard(COMPANY);
        assertNull(timerService.current(COMPANY));
    }

    @Test
    void timerCannotStartOnClosedWork() {
        Project p = hourly();
        var t = task(p, "Done");
        t.changeStatus(TaskStatus.DONE, java.time.Instant.now(clock));
        assertThrows(TimesheetDomainException.class,
                () -> timerService.start(COMPANY, new StartTimerCommand(p.getId().getId(), t.getId().getId(), null)));
    }

    @Test
    void timerTooShortIsRefused() {
        timerService.start(COMPANY, new StartTimerCommand(hourly().getId().getId(), null, null));
        assertEquals("error.timesheet.timerTooShort", assertThrows(TimesheetDomainException.class,
                () -> timerService.stop(COMPANY, null)).getMessageKey());
    }

    // ---- TSH-07 visibility

    @Test
    void costIsOnlyVisibleToTheOwnerAndCostViewers() {
        costs.perHour = new BigDecimal("1000");
        entryService.log(COMPANY, log(TODAY, hourly(), null, 60));
        WeekSummaryResponse s = submitMyWeek();
        permissions.add(TimesheetPermissions.APPROVE_ALL);
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        weekService.approve(COMPANY, s.weekId());
        var mine = entryService.list(COMPANY, null, false, null, null, null, null, null).items().get(0);
        assertNotNull(mine.costAmount(), "an employee sees their own cost (D15)");

        var other = employees.add("Beth", null);
        var e = entries.store.values().iterator().next();
        var theirs = com.bradox.erp.timesheet.domain.core.entity.Entry.restore(e.getId(), COMPANY, other.id(), e.getWorkDate(),
                e.getMinutes(), e.getProjectId(), null, null, false, e.getWeekId(), EntrySource.MANUAL, e.getCreatedAt(), "x",
                e.getUpdatedAt(), new BigDecimal("500"), "IQD", new BigDecimal("500"), false, null, null, null);
        entries.save(theirs);
        permissions.add(TimesheetPermissions.ENTRY_VIEW_ALL);
        var seen = assembler.toResponse(COMPANY, theirs, null);
        assertNull(seen.costAmount(), "someone else's cost is hidden without tsh.cost.view");
        permissions.add(TimesheetPermissions.COST_VIEW);
        assertNotNull(assembler.toResponse(COMPANY, theirs, null).costAmount());
        assertThrows(ForbiddenException.class, () -> {
            permissions.remove(TimesheetPermissions.COST_VIEW);
            costService.profitability(COMPANY, theirs.getProjectId().getId());
        });
    }

    @Test
    void profitabilityTotalsCostAndHours() {
        costs.perHour = new BigDecimal("1000");
        Project p = hourly();
        entryService.log(COMPANY, log(TODAY, p, null, 120));
        WeekSummaryResponse s = submitMyWeek();
        permissions.addAll(List.of(TimesheetPermissions.APPROVE_ALL, TimesheetPermissions.COST_VIEW));
        var st = access.settings(COMPANY);
        st.updateWorkflow(true, null, 0, RoundingMode.NEAREST);
        settings.save(st);
        weekService.approve(COMPANY, s.weekId());
        var pr = costService.profitability(COMPANY, p.getId().getId());
        assertEquals(120, pr.loggedMinutes());
        assertEquals(120, pr.billableMinutes());
        assertEquals(new BigDecimal("2000.0000"), pr.laborCost());
        assertEquals(0, pr.missingCostEntries());
    }

    // ---- TSH-09

    @Test
    void ensureTaskIsIdempotentAndEntriesCarryTheRecord() {
        UUID record = UUID.randomUUID();
        UUID t1 = targetService.ensureTask(COMPANY, "repair.order", record, "RO-0001", null);
        UUID t2 = targetService.ensureTask(COMPANY, "repair.order", record, "RO-0001", null);
        assertEquals(t1, t2);
        assertEquals(1, projects.store.values().stream().filter(Project::isSystemManaged).count());
        assertEquals("Workshop", projects.store.values().stream().filter(Project::isSystemManaged).findFirst().orElseThrow().getName());

        EntryResponse logged = entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(null,
                TODAY, null, t1, 60, null, null, null));
        assertEquals(record, entries.find(new com.bradox.erp.timesheet.domain.core.valueobject.EntryId(logged.id())).orElseThrow().getRecordId());
        assertEquals(60, entryService.byRecord(COMPANY, "repair.order", record).totalMinutes());
        assertEquals(0, entryService.byRecord(COMPANY, "repair.order", UUID.randomUUID()).items().size());
    }

    @Test
    void closingTheRecordClosesTheTaskAndBlocksNewTime() {
        UUID record = UUID.randomUUID();
        UUID task = targetService.ensureTask(COMPANY, "repair.order", record, "RO-0002", null);
        targetService.closeTask(COMPANY, "repair.order", record, false);
        assertEquals("error.timesheet.taskClosed", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.EntryCommand(null, TODAY,
                        null, task, 60, null, null, null))).getMessageKey());
        UUID other = UUID.randomUUID();
        targetService.ensureTask(COMPANY, "repair.order", other, "RO-0003", null);
        targetService.archiveTask(COMPANY, "repair.order", other);
        assertEquals(TaskStatus.CANCELED, tasks.findByRecord(COMPANY, "repair.order", other).orElseThrow().getStatus());
        targetService.closeTask(COMPANY, "repair.order", UUID.randomUUID(), false);   // unknown record: no-op
    }

    @Test
    void tasksOfASystemManagedProjectCannotBeCreatedByHand() {
        targetService.ensureTask(COMPANY, "repair.order", UUID.randomUUID(), "RO-9", null);
        UUID projectId = projects.store.values().stream().filter(Project::isSystemManaged).findFirst().orElseThrow().getId().getId();
        permissions.add(TimesheetPermissions.TASK_MANAGE);
        assertEquals("error.timesheet.projectSystemManaged", assertThrows(TimesheetDomainException.class,
                () -> taskService.create(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.TaskCommand(projectId, "By hand",
                        null, null, null, null))).getMessageKey());
    }
}
