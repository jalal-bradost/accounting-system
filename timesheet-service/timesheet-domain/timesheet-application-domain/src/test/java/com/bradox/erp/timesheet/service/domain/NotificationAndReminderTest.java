package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TSH-05 #1 and #5 notifications, and the weekly reminder (#8), over fakes. */
class NotificationAndReminderTest extends ServiceTestBase {

    private UUID bossUser;
    private UUID workerUser;
    private EmployeeRef boss;
    private EmployeeRef worker;
    private Project project;

    /** A worker whose HR manager is the boss; both have platform users. The caller starts out as the worker. */
    @BeforeEach
    void people() {
        bossUser = UUID.randomUUID();
        workerUser = UUID.randomUUID();
        boss = employees.add("Boss", bossUser);
        worker = employees.add("Worker", workerUser, boss.id(), null);
        project = project("Website", BillingMode.HOURLY, false);
        permissions.addAll(List.of(TimesheetPermissions.APPROVE, TimesheetPermissions.ENTRY_VIEW_TEAM, TimesheetPermissions.SETTINGS_MANAGE));
        actAs(worker);
    }

    /** Signs the fake request in as the given employee. */
    private void actAs(EmployeeRef employee) {
        employees.byUser.clear();
        employees.byUser.put(userId, employee.id());
    }

    private WeekSummaryResponse submitWorkersWeek() {
        actAs(worker);
        entryService.log(COMPANY, log(TODAY, project, null, 120));
        return weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
    }

    // ---------------------------------------------------------------- notifications

    @Test
    void submittingAssignsATodoToTheManager() {
        WeekSummaryResponse w = submitWorkersWeek();
        var todos = notifications.todos;
        assertEquals(1, todos.size());
        assertEquals(bossUser.toString(), todos.get(0).assignee());
        assertEquals(TimesheetNotifier.WEEK_MODEL, todos.get(0).model());
        assertEquals(w.weekId(), todos.get(0).record());
        assertTrue(todos.get(0).subject().contains("Worker"));
        assertTrue(todos.get(0).body().contains("2h"));
    }

    @Test
    void withoutAManagerTheDepartmentManagerThenTheTimesheetManagersAreAsked() {
        UUID deptManagerUser = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        EmployeeRef deptManager = employees.add("Dept manager", deptManagerUser);
        employees.departmentManagers.put(deptId, deptManager.id());
        EmployeeRef solo = employees.add("Solo", UUID.randomUUID(), null, deptId);
        actAs(solo);
        entryService.log(COMPANY, log(TODAY, project, null, 60));
        weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
        assertEquals(1, notifications.to(deptManagerUser).size());

        // no manager and no department: the users who hold tsh.approve_all are asked, but never the owner themselves
        notifications.todos.clear();
        UUID aloneUser = UUID.randomUUID();
        EmployeeRef alone = employees.add("Alone", aloneUser);
        actAs(alone);
        notifications.approvers.addAll(List.of("approver-one", "approver-two", aloneUser.toString()));
        entryService.log(COMPANY, log(TODAY, project, null, 60));
        weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
        assertEquals(List.of("approver-one", "approver-two"), notifications.todos.stream().map(t -> t.assignee()).toList());
    }

    @Test
    void approvalClosesTheApproversTodo() {
        WeekSummaryResponse w = submitWorkersWeek();
        actAs(boss);
        notifications.completed.clear();   // submitting also clears older notices on the week
        weekService.approve(COMPANY, w.weekId());
        assertEquals(List.of(TimesheetNotifier.WEEK_MODEL + ":" + w.weekId()), notifications.completed);
    }

    @Test
    void refusalTellsTheEmployeeWhyAndClosesTheApproversTodo() {
        WeekSummaryResponse w = submitWorkersWeek();
        notifications.todos.clear();
        actAs(boss);
        notifications.completed.clear();
        weekService.refuse(COMPANY, w.weekId(), new WeekReasonCommand("Tuesday is missing"));
        assertEquals(1, notifications.completed.size());
        var toWorker = notifications.to(workerUser);
        assertEquals(1, toWorker.size());
        assertTrue(toWorker.get(0).body().contains("Tuesday is missing"));
        assertTrue(toWorker.get(0).subject().startsWith("Timesheet refused"));
    }

    @Test
    void reopeningNotifiesTheEmployee() {
        WeekSummaryResponse w = submitWorkersWeek();
        actAs(boss);
        permissions.addAll(List.of(TimesheetPermissions.APPROVE_ALL, TimesheetPermissions.REOPEN));
        weekService.approve(COMPANY, w.weekId());
        notifications.todos.clear();
        weekService.reopen(COMPANY, w.weekId(), new WeekReasonCommand("wrong hours"));
        var toWorker = notifications.to(workerUser);
        assertEquals(1, toWorker.size());
        assertTrue(toWorker.get(0).body().contains("wrong hours"));
    }

    @Test
    void aBrokenNotificationNeverBlocksTheWorkflow() {
        notifications.failWith = new IllegalStateException("inbox is down");
        WeekSummaryResponse w = submitWorkersWeek();
        assertEquals(com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.SUBMITTED, w.status());
        actAs(boss);
        assertEquals(com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.APPROVED, weekService.approve(COMPANY, w.weekId()).status());
    }

    @Test
    void anApproverWithoutAUserGetsNothingAndNothingBreaks() {
        EmployeeRef orphanBoss = employees.add("No account", null);
        EmployeeRef report = employees.add("Report", UUID.randomUUID(), orphanBoss.id(), null);
        actAs(report);
        entryService.log(COMPANY, log(TODAY, project, null, 60));
        weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
        assertTrue(notifications.todos.isEmpty());
    }

    // ---------------------------------------------------------------- reminders

    private void logOldWeek(int weeksAgo, boolean submit) {
        LocalDate day = TODAY.minusDays(7L * weeksAgo);
        entryService.log(COMPANY, log(day, project, null, 60));
        if (submit) {
            weekService.submit(COMPANY, new SubmitWeekCommand(null, day));
        }
    }

    @Test
    void anEmployeeWithAnUnsubmittedFinishedWeekGetsOneTodoAndTheManagerOneSummary() {
        expected.noSchedule = true;       // only logged time counts, so the outcome does not depend on the schedule
        actAs(worker);
        logOldWeek(1, false);
        var result = reminderService.run(COMPANY);
        assertEquals(1, result.employeesReminded());
        assertEquals(1, result.managersNotified());
        var mine = notifications.to(workerUser);
        assertEquals(1, mine.size());
        assertEquals(ReminderApplicationServiceImpl.EMPLOYEE_MODEL, mine.get(0).model());
        assertEquals(worker.id(), mine.get(0).record());
        assertTrue(mine.get(0).body().contains("1 week"));
        var team = notifications.to(bossUser);
        assertEquals(1, team.size());
        assertEquals(ReminderApplicationServiceImpl.TEAM_MODEL, team.get(0).model());
        assertTrue(team.get(0).body().contains("Worker (1 week)"));
    }

    @Test
    void theSameWeekIsNeverRemindedTwice() {
        expected.noSchedule = true;
        logOldWeek(1, false);
        assertEquals(1, reminderService.run(COMPANY).employeesReminded());
        var again = reminderService.run(COMPANY);
        assertEquals(0, again.employeesReminded());
        assertEquals(0, again.managersNotified());
        assertEquals(2, notifications.todos.size(), "one for the employee, one summary for the manager, nothing more");
    }

    @Test
    void submittedWeeksAndEmptyWeeksAreLeftAlone() {
        expected.noSchedule = true;
        logOldWeek(1, true);              // submitted
        // week 2 has nothing logged and nothing expected; week 3 is older than... still inside the window but empty
        var result = reminderService.run(COMPANY);
        assertEquals(0, result.employeesReminded());
        assertTrue(notifications.todos.stream().noneMatch(t -> t.model().equals(ReminderApplicationServiceImpl.EMPLOYEE_MODEL)));
    }

    @Test
    void expectedHoursWithNothingLoggedAlsoCountAsMissing() {
        // the default fake schedule expects 8 h on weekdays, so a person who logged nothing is behind
        var result = reminderService.run(COMPANY);
        assertTrue(result.employeesReminded() >= 1);
        assertTrue(notifications.to(workerUser).get(0).body().contains("4 weeks"), "the look-back window is four finished weeks");
    }

    @Test
    void weeksOlderThanTheWindowAreIgnored() {
        expected.noSchedule = true;
        logOldWeek(6, false);
        assertEquals(0, reminderService.run(COMPANY).employeesReminded());
    }

    @Test
    void employeesWithoutAUserAreSkipped() {
        expected.noSchedule = true;
        employees.byUser.clear();
        employees.userOfEmployee.clear();
        assertEquals(0, reminderService.run(COMPANY).employeesReminded());
    }

    @Test
    void theJobOnlyRunsOnTheConfiguredWeekday() {
        expected.noSchedule = true;
        logOldWeek(1, false);
        var st = access.settings(COMPANY);
        st.updateReminder(true, DayOfWeek.MONDAY);       // today is a Thursday
        settings.save(st);
        jobs.sendReminders();
        assertTrue(notifications.todos.isEmpty());
        st.updateReminder(true, DayOfWeek.THURSDAY);
        settings.save(st);
        jobs.sendReminders();
        assertFalse(notifications.todos.isEmpty());
        int sent = notifications.todos.size();
        jobs.sendReminders();
        assertEquals(sent, notifications.todos.size(), "running the job again the same day changes nothing");
    }

    @Test
    void aTurnedOffReminderSendsNothing() {
        expected.noSchedule = true;
        logOldWeek(1, false);
        var st = access.settings(COMPANY);
        st.updateReminder(false, DayOfWeek.THURSDAY);
        settings.save(st);
        jobs.sendReminders();
        assertTrue(notifications.todos.isEmpty());
    }

    @Test
    void sendingNowNeedsTheSettingsPermissionAndIgnoresTheWeekday() {
        expected.noSchedule = true;
        logOldWeek(1, false);
        var st = access.settings(COMPANY);
        st.updateReminder(true, DayOfWeek.MONDAY);
        settings.save(st);
        permissions.remove(TimesheetPermissions.SETTINGS_MANAGE);
        assertThrows(ForbiddenException.class, () -> reminderService.runNow(COMPANY));
        permissions.add(TimesheetPermissions.SETTINGS_MANAGE);
        assertEquals(1, reminderService.runNow(COMPANY).employeesReminded());
    }

    @Test
    void settingsCarryTheReminderOptions() {
        var s = settingsService.update(COMPANY, new com.bradox.erp.timesheet.service.domain.dto.SettingsCommand(null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, false, DayOfWeek.FRIDAY));
        assertFalse(s.reminderEnabled());
        assertEquals(DayOfWeek.FRIDAY, s.reminderWeekday());
        assertTrue(settingsService.get(COMPANY).managersMaySelfApprove() == false, "other options untouched");
    }
}
