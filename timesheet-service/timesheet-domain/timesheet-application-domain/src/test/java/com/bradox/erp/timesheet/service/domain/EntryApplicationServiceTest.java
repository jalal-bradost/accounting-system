package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.dto.CopyEntriesCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryApplicationServiceTest extends ServiceTestBase {

    @Test
    void logsTimeIntoTheDraftWeekOfThatDate() {
        Project p = project("Website", BillingMode.HOURLY, true);
        EntryResponse r = entryService.log(COMPANY, log(TODAY, p, null, 90));
        assertEquals(90, r.minutes());
        assertEquals(me.id(), r.employeeId());
        assertEquals(EntrySource.MANUAL, r.source());
        assertTrue(r.billable(), "billable defaults from the project");
        assertTrue(r.editable());
        // Saturday week start (D8): Thursday Oct 8 belongs to the week of Oct 3
        TimesheetWeek w = weeks.store.values().iterator().next();
        assertEquals(LocalDate.of(2026, 10, 3), w.getWeekStart());
    }

    @Test
    void freeTextDurationWins() {
        Project p = project("Website", BillingMode.HOURLY, false);
        EntryResponse r = entryService.log(COMPANY, new EntryCommand(null, TODAY, p.getId().getId(), null, 5, "1h30",
                null, null));
        assertEquals(90, r.minutes());
    }

    @Test
    void taskFillsInTheProjectAndMismatchIsRejected() {
        Project a = project("A", BillingMode.HOURLY, false);
        Project b = project("B", BillingMode.HOURLY, false);
        Task t = task(a, "Design");
        EntryResponse r = entryService.log(COMPANY, new EntryCommand(null, TODAY, null, t.getId().getId(), 30, null, null, null));
        assertEquals(a.getId().getId(), r.projectId());
        assertThrows(TimesheetDomainException.class, () -> entryService.log(COMPANY,
                new EntryCommand(null, TODAY, b.getId().getId(), t.getId().getId(), 30, null, null, null)));
    }

    @Test
    void dayLimitBoundaryAt24HoursAndWarningAt12() {
        Project p = project("Website", BillingMode.HOURLY, false);
        EntryResponse first = entryService.log(COMPANY, log(TODAY, p, null, 720));
        assertNull(first.warning());
        EntryResponse over12 = entryService.log(COMPANY, log(TODAY, p, null, 1));
        assertNotNull(over12.warning(), "past 12 h warns but saves");
        entryService.log(COMPANY, log(TODAY, p, null, 719));          // day is now exactly 1440
        TimesheetDomainException ex = assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, null, 1)));
        assertEquals("error.timesheet.dayLimit", ex.getMessageKey());
    }

    @Test
    void futureDatesBlockedUnlessSettingAllowsThem() {
        Project p = project("Website", BillingMode.HOURLY, false);
        TimesheetDomainException ex = assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY.plusDays(1), p, null, 60)));
        assertEquals("error.timesheet.futureDate", ex.getMessageKey());
        entryService.log(COMPANY, log(TODAY, p, null, 60));            // today is fine
        var s = access.settings(COMPANY);
        s.update(null, null, null, true, 2, 480);
        settings.save(s);
        entryService.log(COMPANY, log(TODAY.plusDays(2), p, null, 60));
        assertThrows(TimesheetDomainException.class, () -> entryService.log(COMPANY, log(TODAY.plusDays(3), p, null, 60)));
    }

    @Test
    void closedProjectsAndTasksTakeNoNewTime() {
        Project p = project("Website", BillingMode.HOURLY, false);
        Task t = task(p, "Design");
        t.changeStatus(TaskStatus.DONE, java.time.Instant.now(clock));
        assertEquals("error.timesheet.taskClosed", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, t, 60))).getMessageKey());
        p.archive();
        assertEquals("error.timesheet.projectClosed", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, null, 60))).getMessageKey());
    }

    @Test
    void submittedWeekIsReadOnly() {
        Project p = project("Website", BillingMode.HOURLY, false);
        EntryResponse r = entryService.log(COMPANY, log(TODAY, p, null, 60));
        weeks.store.put(new WeekId(r.weekId()), TimesheetWeek.restore(new WeekId(r.weekId()), COMPANY, me.id(),
                LocalDate.of(2026, 10, 3), com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.SUBMITTED, false,
                java.time.Instant.now(clock)));
        assertEquals("error.timesheet.weekReadOnly", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, null, 30))).getMessageKey());
        assertEquals("error.timesheet.weekReadOnly", assertThrows(TimesheetDomainException.class,
                () -> entryService.update(COMPANY, r.id(), log(TODAY, p, null, 30))).getMessageKey());
        assertEquals("error.timesheet.weekReadOnly", assertThrows(TimesheetDomainException.class,
                () -> entryService.delete(COMPANY, r.id())).getMessageKey());
        assertFalse(entryService.get(COMPANY, r.id()).editable());
    }

    @Test
    void lockedWeekIsReadOnlyEvenIfDraft() {
        Project p = project("Website", BillingMode.HOURLY, false);
        EntryResponse r = entryService.log(COMPANY, log(TODAY, p, null, 60));
        weeks.store.put(new WeekId(r.weekId()), TimesheetWeek.restore(new WeekId(r.weekId()), COMPANY, me.id(),
                LocalDate.of(2026, 10, 3), com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus.DRAFT, true,
                java.time.Instant.now(clock)));
        assertThrows(TimesheetDomainException.class, () -> entryService.delete(COMPANY, r.id()));
    }

    @Test
    void billableRules() {
        Project fixed = project("Fixed", BillingMode.FIXED_PRICE, false);
        assertEquals("error.timesheet.billableFixedPrice", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, new EntryCommand(null, TODAY, fixed.getId().getId(), null, 60, null,
                        null, true))).getMessageKey());
        Project hourly = project("Hourly", BillingMode.HOURLY, true);
        EntryResponse off = entryService.log(COMPANY, new EntryCommand(null, TODAY, hourly.getId().getId(), null, 60,
                null, null, false));
        assertFalse(off.billable(), "the user may switch billable off");
    }

    @Test
    void updateMovesAcrossWeeksAndRechecksDayTotals() {
        Project p = project("Website", BillingMode.HOURLY, false);
        EntryResponse a = entryService.log(COMPANY, log(TODAY, p, null, 600));
        entryService.log(COMPANY, log(TODAY.minusDays(7), p, null, 600));
        // moving 600 min onto a day that already has 600 passes 1200 (warn, allowed)
        EntryResponse moved = entryService.update(COMPANY, a.id(), log(TODAY.minusDays(7), p, null, 600));
        assertEquals(TODAY.minusDays(7), moved.workDate());
        assertNotNull(moved.warning());
        assertEquals(2, weeks.store.size());
        // editing an entry does not count itself twice
        EntryResponse same = entryService.update(COMPANY, moved.id(), log(TODAY.minusDays(7), p, null, 840));
        assertEquals(840, same.minutes());
        assertEquals(1440, entries.minutesOnDay(COMPANY, me.id(), TODAY.minusDays(7), null));
    }

    @Test
    void cannotTouchAnotherEmployeesEntryWithoutPermission() {
        Project p = project("Website", BillingMode.HOURLY, false);
        var other = employees.add("Beth Evans", null);
        permissions.add(TimesheetPermissions.ENTRY_ON_BEHALF);
        EntryResponse theirs = entryService.log(COMPANY, new EntryCommand(other.id(), TODAY, p.getId().getId(), null,
                60, null, null, null));
        assertEquals(EntrySource.ON_BEHALF, theirs.source());
        permissions.remove(TimesheetPermissions.ENTRY_ON_BEHALF);
        assertThrows(ForbiddenException.class, () -> entryService.update(COMPANY, theirs.id(), log(TODAY, p, null, 30)));
        assertThrows(ForbiddenException.class, () -> entryService.delete(COMPANY, theirs.id()));
        assertThrows(ForbiddenException.class, () -> entryService.log(COMPANY, new EntryCommand(other.id(), TODAY,
                p.getId().getId(), null, 60, null, null, null)));
        assertThrows(ForbiddenException.class, () -> entryService.get(COMPANY, theirs.id()));
    }

    @Test
    void onBehalfEntryIsAudited() {
        Project p = project("Website", BillingMode.HOURLY, false);
        var other = employees.add("Beth Evans", null);
        permissions.add(TimesheetPermissions.ENTRY_ON_BEHALF);
        entryService.log(COMPANY, new EntryCommand(other.id(), TODAY, p.getId().getId(), null, 60, null, null, null));
        org.mockito.Mockito.verify(audit).recordBusinessEvent(org.mockito.ArgumentMatchers.eq(COMPANY),
                org.mockito.ArgumentMatchers.eq("tsh.entry"), org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq("Time entry created on behalf"),
                org.mockito.ArgumentMatchers.argThat(m -> "tester".equals(m.get("onBehalfBy"))));
    }

    @Test
    void userWithoutEmployeeCannotLogTime() {
        employees.byUser.clear();
        Project p = project("Website", BillingMode.HOURLY, false);
        assertEquals("error.timesheet.noEmployee", assertThrows(TimesheetDomainException.class,
                () -> entryService.log(COMPANY, log(TODAY, p, null, 60))).getMessageKey());
        assertEquals(0, entryService.list(COMPANY, null, false, null, null, null, null, null).items().size());
    }

    @Test
    void listIsScopedToMeUnlessViewAll() {
        Project p = project("Website", BillingMode.HOURLY, true);
        var other = employees.add("Beth Evans", null);
        entryService.log(COMPANY, log(TODAY, p, null, 60));
        permissions.add(TimesheetPermissions.ENTRY_ON_BEHALF);
        entryService.log(COMPANY, new EntryCommand(other.id(), TODAY, p.getId().getId(), null, 120, null, null, null));
        permissions.remove(TimesheetPermissions.ENTRY_ON_BEHALF);

        var mine = entryService.list(COMPANY, null, false, null, null, null, null, null);
        assertEquals(1, mine.items().size());
        assertEquals(60, mine.totalMinutes());
        assertEquals(60, mine.billableMinutes());
        assertThrows(ForbiddenException.class, () -> entryService.list(COMPANY, null, true, null, null, null, null, null));
        assertThrows(ForbiddenException.class, () -> entryService.list(COMPANY, other.id(), false, null, null, null, null, null));

        permissions.add(TimesheetPermissions.ENTRY_VIEW_ALL);
        var all = entryService.list(COMPANY, null, true, null, null, null, null, null);
        assertEquals(2, all.items().size());
        assertEquals(180, all.totalMinutes());
    }

    @Test
    void repeatYesterdayCopiesAndSkipsClosedTasks() {
        Project p = project("Website", BillingMode.HOURLY, false);
        Task open = task(p, "Design");
        Task closing = task(p, "Review");
        LocalDate yesterday = TODAY.minusDays(1);
        entryService.log(COMPANY, log(yesterday, p, open, 60));
        entryService.log(COMPANY, log(yesterday, p, closing, 30));
        closing.changeStatus(TaskStatus.DONE, java.time.Instant.now(clock));
        var copied = entryService.copy(COMPANY, new CopyEntriesCommand(null, yesterday, TODAY));
        assertEquals(1, copied.size());
        assertEquals(60, copied.get(0).minutes());
        assertEquals(TODAY, copied.get(0).workDate());
        assertThrows(TimesheetDomainException.class,
                () -> entryService.copy(COMPANY, new CopyEntriesCommand(null, TODAY, TODAY)));
    }
}
