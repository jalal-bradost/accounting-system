package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.rule.ReminderRule;
import com.bradox.erp.timesheet.domain.core.rule.ReminderRule.WeekFact;
import com.bradox.erp.timesheet.domain.core.rule.WeekCalendar;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.dto.ReminderRunResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.ReminderApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.ExpectedDay;
import com.bradox.erp.timesheet.service.domain.ports.output.TimesheetNotificationPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReminderLogRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The weekly "submit your timesheet" reminder (TSH-05 #8): each person with unsubmitted finished weeks gets a to-do, and
 * their manager gets one summary of the team. Once per person per week, tracked in the reminder log.
 */
@Service
class ReminderApplicationServiceImpl implements ReminderApplicationService {

    static final String EMPLOYEE_MODEL = "tsh.reminder";
    static final String TEAM_MODEL = "tsh.team-reminder";
    /** How many finished weeks back a reminder looks. Older gaps are a manager's job, not a nag. */
    static final int LOOKBACK_WEEKS = 4;

    private final EmployeeLookupPort employees;
    private final WeekRepository weeks;
    private final EntryRepository entries;
    private final ExpectedHoursPort expectedHours;
    private final ReminderLogRepository log;
    private final TimesheetNotificationPort notifications;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    ReminderApplicationServiceImpl(EmployeeLookupPort employees, WeekRepository weeks, EntryRepository entries,
                                   ExpectedHoursPort expectedHours, ReminderLogRepository log,
                                   TimesheetNotificationPort notifications, AuditLogPort audit, TimesheetAccess access) {
        this.employees = employees;
        this.weeks = weeks;
        this.entries = entries;
        this.expectedHours = expectedHours;
        this.log = log;
        this.notifications = notifications;
        this.audit = audit;
        this.access = access;
    }

    @Override
    public ReminderRunResponse runNow(CompanyId companyId) {
        access.require(TimesheetPermissions.SETTINGS_MANAGE);
        return run(companyId);
    }

    /** Used by the job and by {@link #runNow}. Not transactional: each notification and log row stands on its own. */
    ReminderRunResponse run(CompanyId companyId) {
        TimesheetSettings settings = access.settings(companyId);
        LocalDate today = access.today(settings);
        LocalDate currentWeek = WeekCalendar.weekStart(today, settings.getWeekStartDay());
        List<LocalDate> starts = new ArrayList<>();
        for (int i = LOOKBACK_WEEKS; i >= 1; i--) {
            starts.add(currentWeek.minusDays(7L * i));
        }
        LocalDate oldest = starts.get(0);
        LocalDate newestEnd = starts.get(starts.size() - 1).plusDays(6);

        List<EmployeeRef> people = employees.listActive(companyId);
        Map<UUID, UUID> users = employees.userIds(companyId, people.stream().map(EmployeeRef::id).toList());
        List<EmployeeRef> reachable = people.stream().filter(p -> users.containsKey(p.id())).toList();
        if (reachable.isEmpty()) {
            return new ReminderRunResponse(0, 0);
        }
        Map<String, TimesheetWeek> weekByKey = new HashMap<>();
        List<WeekId> weekIds = new ArrayList<>();
        for (TimesheetWeek w : weeks.search(companyId, null, reachable.stream().map(EmployeeRef::id).collect(Collectors.toSet()),
                oldest, starts.get(starts.size() - 1))) {
            weekByKey.put(w.getEmployeeId() + "@" + w.getWeekStart(), w);
            weekIds.add(w.getId());
        }
        Map<UUID, Long> minutes = entries.minutesByWeek(weekIds);

        int reminded = 0;
        Map<UUID, List<String>> teamLines = new LinkedHashMap<>();
        for (EmployeeRef p : reachable) {
            Map<LocalDate, ExpectedDay> expected = expectedHours.expected(companyId, p.id(), oldest, newestEnd);
            List<WeekFact> facts = new ArrayList<>();
            for (LocalDate start : starts) {
                TimesheetWeek w = weekByKey.get(p.id() + "@" + start);
                int exp = 0;
                for (int d = 0; d < 7; d++) {
                    ExpectedDay day = expected.get(start.plusDays(d));
                    exp += day == null ? 0 : day.minutes();
                }
                facts.add(new WeekFact(start, w == null ? null : w.getStatus(),
                        w != null && minutes.getOrDefault(w.getId().getId(), 0L) > 0, exp));
            }
            List<LocalDate> missing = ReminderRule.missingWeeks(facts);
            if (missing.isEmpty() || log.exists(p.id(), currentWeek, ReminderLogRepository.EMPLOYEE)) {
                continue;
            }
            String weeksText = missing.stream().map(s -> s + " to " + s.plusDays(6)).collect(Collectors.joining("; "));
            try {
                notifications.assignTodo(companyId, EMPLOYEE_MODEL, p.id(), users.get(p.id()).toString(),
                        "Submit your timesheet", "You have " + missing.size() + (missing.size() == 1 ? " week" : " weeks")
                                + " that " + (missing.size() == 1 ? "is" : "are") + " not submitted: " + weeksText
                                + ". Open Timesheets, fill in your hours and press Submit week.", today);
                log.record(companyId, p.id(), currentWeek, ReminderLogRepository.EMPLOYEE);
                reminded++;
                if (p.managerId() != null) {
                    teamLines.computeIfAbsent(p.managerId(), k -> new ArrayList<>())
                            .add(p.name() + " (" + missing.size() + (missing.size() == 1 ? " week" : " weeks") + ")");
                }
            } catch (RuntimeException ex) {
                // One undeliverable reminder must not stop the others, and is retried on the next run.
            }
        }

        int managersNotified = 0;
        Map<UUID, UUID> managerUsers = employees.userIds(companyId, teamLines.keySet());
        for (var e : teamLines.entrySet()) {
            UUID manager = e.getKey();
            if (!managerUsers.containsKey(manager) || log.exists(manager, currentWeek, ReminderLogRepository.MANAGER)) {
                continue;
            }
            try {
                notifications.assignTodo(companyId, TEAM_MODEL, manager, managerUsers.get(manager).toString(),
                        "Team timesheets not submitted", "These people have unsubmitted weeks: " + String.join(", ", e.getValue())
                                + ". Open Timesheets, All Timesheets to follow up.", today);
                log.record(companyId, manager, currentWeek, ReminderLogRepository.MANAGER);
                managersNotified++;
            } catch (RuntimeException ex) {
                // retried on the next run
            }
        }
        if (reminded > 0 || managersNotified > 0) {
            audit.recordBusinessEvent(companyId, "tsh.reminder", companyId.getId(), "Timesheet reminders sent",
                    Map.of("employees", reminded, "managers", managersNotified, "runWeek", currentWeek.toString()));
        }
        return new ReminderRunResponse(reminded, managersNotified);
    }
}
