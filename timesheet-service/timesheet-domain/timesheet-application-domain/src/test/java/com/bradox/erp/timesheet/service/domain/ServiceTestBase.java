package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.web.CompanyContext;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class ServiceTestBase {

    static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    /** Thursday 2026-10-08, the day of the screenshots. With a Saturday week start the week is Oct 3 to Oct 9. */
    static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    final UUID userId = UUID.randomUUID();
    final Set<String> permissions = new HashSet<>(Set.of(TimesheetPermissions.ENTRY_OWN));
    /** A clock tests can move forward, for timers and auto-lock. */
    static final class TestClock extends Clock {
        Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        void advanceMinutes(long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }

    final TestClock clock = new TestClock(Instant.parse("2026-10-08T09:00:00Z"));

    Fakes.ProjectRepo projects;
    Fakes.TaskRepo tasks;
    Fakes.EntryRepo entries;
    Fakes.WeekRepo weeks;
    Fakes.SettingsRepo settings;
    Fakes.LineRepo lines;
    Fakes.Employees employees;
    Fakes.Partners partners;
    Fakes.Timers timers;
    Fakes.Costs costs;
    Fakes.SalesLines salesLines;
    Fakes.Rates rates;
    Fakes.Postings postings;
    Fakes.Ledger ledger;
    Fakes.Notifications notifications;
    Fakes.Attendance attendance;
    Fakes.ReminderLog reminderLog;
    TimesheetNotifier notifier;
    ReminderApplicationServiceImpl reminderService;
    BillingSync billingSync;
    PostingCoordinator postingCoordinator;
    BillingApplicationServiceImpl billingService;
    PostingApplicationServiceImpl postingService;
    Fakes.Expected expected;
    WeekApplicationServiceImpl weekService;
    TimerApplicationServiceImpl timerService;
    CostApplicationServiceImpl costService;
    TimesheetTargetApplicationServiceImpl targetService;
    TimesheetJobs jobs;
    AuditLogPort audit;
    TimesheetAccess access;
    EntryWriter writer;
    EntryAssembler assembler;
    EntryApplicationServiceImpl entryService;
    GridApplicationServiceImpl gridService;
    ProjectApplicationServiceImpl projectService;
    TaskApplicationServiceImpl taskService;
    SettingsApplicationServiceImpl settingsService;
    EmployeeRef me;

    @BeforeEach
    void setUpBase() {
        projects = new Fakes.ProjectRepo();
        tasks = new Fakes.TaskRepo();
        entries = new Fakes.EntryRepo();
        weeks = new Fakes.WeekRepo();
        settings = new Fakes.SettingsRepo();
        lines = new Fakes.LineRepo();
        employees = new Fakes.Employees();
        partners = new Fakes.Partners();
        timers = new Fakes.Timers();
        costs = new Fakes.Costs();
        salesLines = new Fakes.SalesLines();
        rates = new Fakes.Rates();
        postings = new Fakes.Postings();
        ledger = new Fakes.Ledger();
        notifications = new Fakes.Notifications();
        attendance = new Fakes.Attendance();
        reminderLog = new Fakes.ReminderLog();
        entries.weeks = weeks;
        postings.weeks = weeks;
        expected = new Fakes.Expected();
        me = employees.add("Abigail Peterson", userId);
        CompanyContext ctx = mock(CompanyContext.class);
        when(ctx.currentUser()).thenAnswer(i -> Optional.of(new UserId(userId)));
        when(ctx.currentUserDisplay()).thenReturn("tester");
        audit = mock(AuditLogPort.class);
        AuthorizationPort auth = new AuthorizationPort() {
            @Override
            public boolean hasAll(UserId user, Set<String> required) {
                return permissions.containsAll(required);
            }

            @Override
            public boolean hasAny(UserId user, Set<String> required) {
                return required.stream().anyMatch(permissions::contains);
            }
        };
        access = new TimesheetAccess(ctx, auth, employees, settings, clock);
        writer = new EntryWriter(entries, projects, tasks, weeks, audit, access);
        billingSync = new BillingSync(entries, projects, tasks, rates, salesLines, salesLines, audit, access);
        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<org.springframework.transaction.PlatformTransactionManager> noTx =
                mock(org.springframework.beans.factory.ObjectProvider.class);
        postingCoordinator = new PostingCoordinator(postings, weeks, entries, projects, employees, ledger, audit, access, noTx);
        assembler = new EntryAssembler(projects, tasks, weeks, employees, access, billingSync);
        entryService = new EntryApplicationServiceImpl(entries, writer, assembler, access);
        gridService = new GridApplicationServiceImpl(entries, projects, tasks, weeks, lines, expected,
                writer, access, employees, attendance);
        notifier = new TimesheetNotifier(notifications, employees, access);
        reminderService = new ReminderApplicationServiceImpl(employees, weeks, entries, expected, reminderLog, notifications, audit, access);
        weekService = new WeekApplicationServiceImpl(weeks, entries, employees, expected, costs, audit, access,
                billingSync, postingCoordinator, postings, notifier);
        billingService = new BillingApplicationServiceImpl(projects, tasks, entries, weeks, rates, employees, salesLines,
                billingSync, assembler, audit, access);
        postingService = new PostingApplicationServiceImpl(postings, weeks, projects, employees, postingCoordinator, audit, access,
                entries, ledger);
        timerService = new TimerApplicationServiceImpl(timers, projects, tasks, writer, assembler, audit, access);
        costService = new CostApplicationServiceImpl(projects, entries, costs, audit, access);
        targetService = new TimesheetTargetApplicationServiceImpl(projects, tasks, access);
        jobs = new TimesheetJobs(settings, weeks, audit, access, postings, postingCoordinator, reminderService, postingService);
        projectService = new ProjectApplicationServiceImpl(projects, entries, employees, partners, audit, access, salesLines);
        taskService = new TaskApplicationServiceImpl(tasks, projects, entries, employees, audit, access, salesLines);
        settingsService = new SettingsApplicationServiceImpl(settings, employees, audit, access, ledger);
    }

    Project project(String name, BillingMode mode, boolean billable) {
        return projects.save(Project.create(new ProjectId(UUID.randomUUID()), COMPANY, name, null, null, null, true,
                mode, billable, null, null, null, Instant.now(clock), "system"));
    }

    Task task(Project p, String name) {
        return tasks.save(Task.create(new TaskId(UUID.randomUUID()), COMPANY, p.getId(), name, null, null, null, null,
                Instant.now(clock), "system"));
    }

    static EntryCommand log(LocalDate date, Project p, Task t, int minutes) {
        return new EntryCommand(null, date, p.getId().getId(), t == null ? null : t.getId().getId(), minutes, null,
                null, null);
    }
}
