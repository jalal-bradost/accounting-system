package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.service.domain.dto.TaskCommand;
import com.bradox.erp.timesheet.service.domain.dto.TaskResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskStatusCommand;
import com.bradox.erp.timesheet.service.domain.ports.input.TaskApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
class TaskApplicationServiceImpl implements TaskApplicationService {

    static final String AUDIT_MODEL = "tsh.task";

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final EntryRepository entries;
    private final EmployeeLookupPort employees;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final SalesLineLookupPort salesLines;

    TaskApplicationServiceImpl(TaskRepository tasks, ProjectRepository projects, EntryRepository entries,
                               EmployeeLookupPort employees, AuditLogPort audit, TimesheetAccess access,
                               SalesLineLookupPort salesLines) {
        this.tasks = tasks;
        this.projects = projects;
        this.entries = entries;
        this.employees = employees;
        this.audit = audit;
        this.access = access;
        this.salesLines = salesLines;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(CompanyId companyId, UUID projectId) {
        loadProject(companyId, projectId);
        return toResponses(companyId, tasks.findByProject(companyId, new ProjectId(projectId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listOpen(CompanyId companyId) {
        List<ProjectId> active = projects.findAll(companyId, false).stream()
                .filter(Project::acceptsEntries).map(Project::getId).toList();
        return toResponses(companyId, tasks.findOpenByProjects(companyId, active));
    }

    @Override
    @Transactional
    public TaskResponse create(CompanyId companyId, TaskCommand command) {
        access.require(TimesheetPermissions.TASK_MANAGE);
        if (command.projectId() == null) {
            throw new TimesheetDomainException("error.timesheet.projectRequired", null, "A project is required");
        }
        Project project = loadProject(companyId, command.projectId());
        if (project.isSystemManaged()) {
            throw new TimesheetDomainException("error.timesheet.projectSystemManaged", null,
                    "Tasks of this project are created by the linked module, not by hand");
        }
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new TimesheetDomainException("error.timesheet.projectClosed", null, "This project is archived");
        }
        checkAssignees(companyId, command.assigneeEmployeeIds());
        Task t = Task.create(new TaskId(UUID.randomUUID()), companyId, project.getId(), command.name(),
                command.description(), command.assigneeEmployeeIds(), command.allocatedMinutes(), command.deadline(),
                access.clock().instant(), access.actorLabel());
        applySaleLine(companyId, t, command.saleLineId());
        Task saved = tasks.save(t);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Task created", summary(saved));
        return toResponses(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public TaskResponse update(CompanyId companyId, UUID id, TaskCommand command) {
        access.require(TimesheetPermissions.TASK_MANAGE);
        Task t = load(companyId, id);
        checkAssignees(companyId, command.assigneeEmployeeIds());
        t.update(command.name(), command.description(), command.assigneeEmployeeIds(), command.allocatedMinutes(),
                command.deadline());
        applySaleLine(companyId, t, command.saleLineId());
        Task saved = tasks.save(t);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Task updated", summary(saved));
        return toResponses(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(CompanyId companyId, UUID id, TaskStatusCommand command) {
        access.require(TimesheetPermissions.TASK_MANAGE);
        Task t = load(companyId, id);
        var before = t.getStatus();
        t.changeStatus(command.status(), access.clock().instant());
        Task saved = tasks.save(t);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Task status changed",
                Map.of("from", before.name(), "to", saved.getStatus().name()));
        return toResponses(companyId, List.of(saved)).get(0);
    }

    /** TSH-06: the task's order line is first in the resolution order; changing it needs billing rights. */
    private void applySaleLine(CompanyId companyId, Task t, UUID line) {
        if (java.util.Objects.equals(line, t.getSaleLineId())) {
            return;
        }
        access.require(TimesheetPermissions.BILLING_MANAGE);
        if (line != null) {
            var info = salesLines.find(companyId, line).orElseThrow(() -> new TimesheetDomainException(
                    "error.timesheet.saleLineNotFound", null, "Sales order line not found"));
            if (!info.eligible()) {
                throw new TimesheetDomainException("error.timesheet.saleLineNotEligible", null,
                        "That line is not a service line invoiced from timesheets");
            }
        }
        t.assignSaleLine(line);
    }

    private Task load(CompanyId companyId, UUID id) {
        return tasks.find(new TaskId(id)).filter(t -> t.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Project loadProject(CompanyId companyId, UUID id) {
        return projects.find(new ProjectId(id)).filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private void checkAssignees(CompanyId companyId, Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (employees.findAll(companyId, ids).size() != ids.size()) {
            throw new TimesheetDomainException("error.timesheet.employeeNotFound", null, "An assignee was not found");
        }
    }

    private List<TaskResponse> toResponses(CompanyId companyId, List<Task> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> logged = entries.minutesByTask(companyId, list.stream().map(t -> t.getId().getId()).toList());
        Map<ProjectId, Project> projectById = projects.findByIds(
                list.stream().map(Task::getProjectId).distinct().toList()).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        Map<UUID, EmployeeRef> people = employees.findAll(companyId,
                list.stream().flatMap(t -> t.getAssigneeEmployeeIds().stream()).collect(Collectors.toSet()));
        return list.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).thenComparing(Task::getName))
                .map(t -> {
                    List<UUID> assignees = t.getAssigneeEmployeeIds().stream().sorted().toList();
                    Project p = projectById.get(t.getProjectId());
                    return new TaskResponse(t.getId().getId(), t.getProjectId().getId(), p == null ? null : p.getName(),
                            t.getName(), t.getDescription(), t.getStatus(), assignees,
                            assignees.stream().map(a -> people.containsKey(a) ? people.get(a).name() : "?").toList(),
                            t.getAllocatedMinutes(), logged.getOrDefault(t.getId().getId(), 0L), t.getDeadline(),
                            t.acceptsEntries() && p != null && p.acceptsEntries(), t.getSaleLineId(),
                            t.getSaleLineId() == null ? null : salesLines.find(companyId, t.getSaleLineId())
                                    .map(SalesLineLookupPort.SaleLine::label).orElse(null));
                }).toList();
    }

    private static Map<String, Object> summary(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", t.getName());
        m.put("projectId", t.getProjectId().getId().toString());
        m.put("status", t.getStatus().name());
        return m;
    }
}
