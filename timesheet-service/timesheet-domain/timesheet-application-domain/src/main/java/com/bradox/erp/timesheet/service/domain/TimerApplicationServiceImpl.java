package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.Timer;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.RoundingRule;
import com.bradox.erp.timesheet.domain.core.rule.TimerSplit;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TimerId;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.StartTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.StopTimerCommand;
import com.bradox.erp.timesheet.service.domain.dto.TimerResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.TimerApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TimerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The server-side timer (TSH-04). It keeps running when the browser closes. */
@Service
@Validated
class TimerApplicationServiceImpl implements TimerApplicationService {

    static final String AUDIT_MODEL = "tsh.timer";
    static final int LONG_RUN_MINUTES = 720;

    private final TimerRepository timers;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final EntryWriter writer;
    private final EntryAssembler assembler;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    TimerApplicationServiceImpl(TimerRepository timers, ProjectRepository projects, TaskRepository tasks,
                                EntryWriter writer, EntryAssembler assembler, AuditLogPort audit, TimesheetAccess access) {
        this.timers = timers;
        this.projects = projects;
        this.tasks = tasks;
        this.writer = writer;
        this.assembler = assembler;
        this.audit = audit;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public TimerResponse current(CompanyId companyId) {
        return access.actor(companyId).flatMap(a -> timers.find(companyId, a.id())).map(this::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public TimerResponse start(CompanyId companyId, StartTimerCommand c) {
        EmployeeRef me = access.requireActor(companyId);
        if (timers.find(companyId, me.id()).isPresent()) {
            throw new TimesheetDomainException("error.timesheet.timerRunning", null,
                    "A timer is already running. Stop it before starting another.");
        }
        Project project = projects.find(new ProjectId(c.projectId())).filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Task task = null;
        if (c.taskId() != null) {
            task = tasks.find(new TaskId(c.taskId())).filter(t -> t.getCompanyId().equals(companyId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
            if (!task.getProjectId().equals(project.getId())) {
                throw new TimesheetDomainException("error.timesheet.taskProjectMismatch", null,
                        "That task belongs to a different project");
            }
        }
        if (!project.acceptsEntries() || (task != null && !task.acceptsEntries())) {
            throw new TimesheetDomainException("error.timesheet.projectClosed", null,
                    "This project or task is closed and takes no new time");
        }
        Timer saved = timers.save(Timer.start(new TimerId(UUID.randomUUID()), companyId, me.id(), project.getId(),
                task == null ? null : task.getId(), c.description(), access.clock().instant()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<EntryResponse> stop(CompanyId companyId, StopTimerCommand c) {
        EmployeeRef me = access.requireActor(companyId);
        Timer timer = timers.find(companyId, me.id()).orElseThrow(() -> new TimesheetDomainException(
                "error.timesheet.noTimer", null, "There is no running timer"));
        TimesheetSettings settings = access.settings(companyId);
        Instant now = access.clock().instant();
        long elapsedMinutes = Duration.between(timer.getStartedAt(), now).toMinutes();
        if (elapsedMinutes > LONG_RUN_MINUTES && !Boolean.TRUE.equals(c == null ? null : c.confirmLong())) {
            throw new TimesheetDomainException("error.timesheet.timerLong", new Object[]{elapsedMinutes},
                    "This timer has run for more than 12 hours. Confirm it or edit the time.");
        }
        List<TimerSplit.Piece> pieces = TimerSplit.split(timer.getStartedAt(), now, settings.getZone()).stream()
                .map(p -> new TimerSplit.Piece(p.date(), RoundingRule.apply(p.minutes(), settings.getRoundingStepMinutes(),
                        settings.getRoundingMode())))
                .filter(p -> p.minutes() > 0).toList();
        if (pieces.isEmpty()) {
            throw new TimesheetDomainException("error.timesheet.timerTooShort", null,
                    "The timer ran for less than a minute. Discard it or let it run longer.");
        }
        String description = c != null && c.description() != null && !c.description().isBlank()
                ? c.description() : timer.getDescription();
        var target = new TimesheetAccess.Target(me, false);
        List<com.bradox.erp.timesheet.domain.core.entity.Entry> created = new ArrayList<>();
        for (TimerSplit.Piece piece : pieces) {
            // A locked week or a closed project throws here and rolls everything back, so the timer stays open (TSH-04 #8).
            created.add(writer.create(companyId, target, piece.date(), piece.minutes(), timer.getProjectId().getId(),
                    timer.getTaskId() == null ? null : timer.getTaskId().getId(), description, null,
                    EntrySource.TIMER).entry());
        }
        timers.delete(companyId, me.id());
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, timer.getId().getId(), "Timer stopped",
                Map.of("entries", created.size(), "minutes", pieces.stream().mapToInt(TimerSplit.Piece::minutes).sum()));
        return assembler.toResponses(companyId, created, null);
    }

    @Override
    @Transactional
    public void discard(CompanyId companyId) {
        EmployeeRef me = access.requireActor(companyId);
        timers.find(companyId, me.id()).ifPresent(t -> {
            timers.delete(companyId, me.id());
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, t.getId().getId(), "Timer discarded", Map.of());
        });
    }

    private TimerResponse toResponse(Timer t) {
        Project p = projects.find(t.getProjectId()).orElse(null);
        Task task = t.getTaskId() == null ? null : tasks.find(t.getTaskId()).orElse(null);
        return new TimerResponse(t.getProjectId().getId(), p == null ? null : p.getName(),
                t.getTaskId() == null ? null : t.getTaskId().getId(), task == null ? null : task.getName(),
                t.getDescription(), t.getStartedAt(),
                Math.max(0, Duration.between(t.getStartedAt(), access.clock().instant()).getSeconds()));
    }
}
