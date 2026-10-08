package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.DayTotalRules;
import com.bradox.erp.timesheet.domain.core.rule.WeekCalendar;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The write rules for time entries (BR-TSH-01 to 07), shared by the entry and grid services so a
 * cell edit and a form edit cannot disagree.
 */
@Component
class EntryWriter {

    static final String AUDIT_MODEL = "tsh.entry";
    static final String WARN_LONG_DAY = "More than 12 hours are logged on this day";

    record Saved(Entry entry, String warning) {
    }

    private record Resolved(Project project, Task task) {
    }

    private final EntryRepository entries;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final WeekRepository weeks;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    EntryWriter(EntryRepository entries, ProjectRepository projects, TaskRepository tasks, WeekRepository weeks,
                AuditLogPort audit, TimesheetAccess access) {
        this.entries = entries;
        this.projects = projects;
        this.tasks = tasks;
        this.weeks = weeks;
        this.audit = audit;
        this.access = access;
    }

    Saved create(CompanyId companyId, TimesheetAccess.Target target, LocalDate date, int minutes, UUID projectId,
                 UUID taskId, String description, Boolean billable) {
        return create(companyId, target, date, minutes, projectId, taskId, description, billable, null);
    }

    /** {@code sourceOverride} lets the timer mark its entries TIMER; otherwise MANUAL or ON_BEHALF. */
    Saved create(CompanyId companyId, TimesheetAccess.Target target, LocalDate date, int minutes, UUID projectId,
                 UUID taskId, String description, Boolean billable, EntrySource sourceOverride) {
        TimesheetSettings settings = access.settings(companyId);
        Resolved resolved = resolve(companyId, projectId, taskId);
        requireAcceptsEntries(resolved);
        checkDate(settings, date);
        Entry.validateMinutes(minutes);
        UUID employeeId = target.employee().id();
        TimesheetWeek week = weekFor(companyId, employeeId, date, settings);
        requireEditable(week);
        String warning = checkDay(companyId, employeeId, date, minutes, null);
        Instant now = access.clock().instant();
        Entry entry = Entry.create(new EntryId(UUID.randomUUID()), companyId, employeeId, date, minutes,
                resolved.project().getId(), resolved.task() == null ? null : resolved.task().getId(), description,
                billableFor(resolved.project(), billable), week.getId(),
                sourceOverride != null ? sourceOverride
                        : target.onBehalf() ? EntrySource.ON_BEHALF : EntrySource.MANUAL, now, access.actorLabel());
        if (resolved.task() != null && resolved.task().isSystemManaged()) {
            entry.linkRecord(resolved.task().getRecordModel(), resolved.task().getRecordId());
        }
        Entry saved = entries.save(entry);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(),
                target.onBehalf() ? "Time entry created on behalf" : "Time entry created",
                changes(saved, target.onBehalf() ? access.actorLabel() : null));
        return new Saved(saved, warning);
    }

    /** {@code billable == null} keeps the entry's current flag. */
    Saved update(CompanyId companyId, Entry existing, LocalDate date, int minutes, UUID projectId, UUID taskId,
                 String description, Boolean billable) {
        TimesheetSettings settings = access.settings(companyId);
        TimesheetWeek currentWeek = weeks.findByIds(java.util.List.of(existing.getWeekId())).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Week not found"));
        requireEditable(currentWeek);

        boolean sameTarget = existing.getProjectId().getId().equals(projectId)
                && java.util.Objects.equals(existing.getTaskId() == null ? null : existing.getTaskId().getId(), taskId);
        Resolved resolved = resolve(companyId, projectId, taskId);
        if (!sameTarget) {
            requireAcceptsEntries(resolved);
        }
        if (!date.equals(existing.getWorkDate())) {
            checkDate(settings, date);
        }
        Entry.validateMinutes(minutes);
        TimesheetWeek week = date.equals(existing.getWorkDate()) ? currentWeek
                : weekFor(companyId, existing.getEmployeeId(), date, settings);
        requireEditable(week);
        String warning = checkDay(companyId, existing.getEmployeeId(), date, minutes, existing.getId());

        boolean bill = billable == null ? existing.isBillable() : billableFor(resolved.project(), billable);
        existing.change(date, minutes, resolved.project().getId(),
                resolved.task() == null ? null : resolved.task().getId(), description, bill, week.getId(),
                access.clock().instant());
        Entry saved = entries.save(existing);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Time entry updated",
                changes(saved, null));
        return new Saved(saved, warning);
    }

    void delete(CompanyId companyId, Entry existing) {
        TimesheetWeek week = weeks.findByIds(java.util.List.of(existing.getWeekId())).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Week not found"));
        requireEditable(week);
        entries.delete(existing.getId());
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, existing.getId().getId(), "Time entry deleted",
                changes(existing, null));
    }

    TimesheetWeek weekFor(CompanyId companyId, UUID employeeId, LocalDate date, TimesheetSettings settings) {
        LocalDate start = WeekCalendar.weekStart(date, settings.getWeekStartDay());
        return weeks.find(companyId, employeeId, start).orElseGet(() -> weeks.save(
                TimesheetWeek.open(new WeekId(UUID.randomUUID()), companyId, employeeId, start,
                        access.clock().instant())));
    }

    private Resolved resolve(CompanyId companyId, UUID projectId, UUID taskId) {
        Task task = null;
        UUID effectiveProject = projectId;
        if (taskId != null) {
            task = tasks.find(new TaskId(taskId)).filter(t -> t.getCompanyId().equals(companyId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
            if (projectId != null && !task.getProjectId().getId().equals(projectId)) {
                throw new TimesheetDomainException("error.timesheet.taskProjectMismatch", null,
                        "That task belongs to a different project");
            }
            effectiveProject = task.getProjectId().getId();
        }
        if (effectiveProject == null) {
            throw new TimesheetDomainException("error.timesheet.projectRequired", null, "A project is required");
        }
        Project project = projects.find(new ProjectId(effectiveProject))
                .filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return new Resolved(project, task);
    }

    /** BR-TSH-04. */
    private void requireAcceptsEntries(Resolved r) {
        if (!r.project().acceptsEntries()) {
            throw new TimesheetDomainException("error.timesheet.projectClosed", null,
                    "This project is archived or does not allow timesheets");
        }
        if (r.task() != null && !r.task().acceptsEntries()) {
            throw new TimesheetDomainException("error.timesheet.taskClosed", null,
                    "This task is done or canceled and takes no new time");
        }
    }

    /** BR-TSH-03. */
    private void checkDate(TimesheetSettings settings, LocalDate date) {
        if (date == null) {
            throw new TimesheetDomainException("error.timesheet.dateRequired", null, "A date is required");
        }
        LocalDate latest = access.today(settings).plusDays(settings.getAllowFutureDays());
        if (date.isAfter(latest)) {
            throw new TimesheetDomainException("error.timesheet.futureDate", new Object[]{date},
                    "You cannot log time for a future date (" + date + ")");
        }
    }

    /** BR-TSH-05. */
    private void requireEditable(TimesheetWeek week) {
        if (!week.isEditable()) {
            String why = week.isLocked() ? "locked" : week.getStatus().name().toLowerCase();
            throw new TimesheetDomainException("error.timesheet.weekReadOnly", new Object[]{why},
                    "This week is " + why + ", so its time cannot be changed");
        }
    }

    /** BR-TSH-02: reject past 24 h, warn past 12 h. */
    private String checkDay(CompanyId companyId, UUID employeeId, LocalDate date, int minutes, EntryId excludeId) {
        int others = entries.minutesOnDay(companyId, employeeId, date, excludeId);
        return switch (DayTotalRules.check(others, minutes)) {
            case REJECT -> throw new TimesheetDomainException("error.timesheet.dayLimit",
                    new Object[]{others, minutes}, "A day cannot pass 24 hours (" + others
                    + " minutes are already logged on " + date + ")");
            case WARN -> WARN_LONG_DAY;
            case OK -> null;
        };
    }

    /** BR-TSH-07: default from the project; switching on is refused for fixed-price projects. */
    private boolean billableFor(Project project, Boolean requested) {
        if (requested == null) {
            return project.isBillableDefault();
        }
        if (requested && project.getBillingMode() == BillingMode.FIXED_PRICE) {
            throw new TimesheetDomainException("error.timesheet.billableFixedPrice", null,
                    "Hours on a fixed-price project are cost only and cannot be billable");
        }
        return requested;
    }

    private static Map<String, Object> changes(Entry e, String onBehalfBy) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("employeeId", e.getEmployeeId().toString());
        m.put("workDate", e.getWorkDate().toString());
        m.put("minutes", e.getMinutes());
        m.put("projectId", e.getProjectId().getId().toString());
        if (e.getTaskId() != null) {
            m.put("taskId", e.getTaskId().getId().toString());
        }
        m.put("billable", e.isBillable());
        if (onBehalfBy != null) {
            m.put("onBehalfBy", onBehalfBy);
        }
        return m;
    }
}
