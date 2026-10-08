package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns entries into responses with names resolved in a handful of batch lookups, not one per row. */
@Component
class EntryAssembler {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final WeekRepository weeks;
    private final EmployeeLookupPort employees;
    private final TimesheetAccess access;
    private final BillingSync billingSync;

    EntryAssembler(ProjectRepository projects, TaskRepository tasks, WeekRepository weeks,
                   EmployeeLookupPort employees, TimesheetAccess access, BillingSync billingSync) {
        this.projects = projects;
        this.tasks = tasks;
        this.weeks = weeks;
        this.employees = employees;
        this.access = access;
        this.billingSync = billingSync;
    }

    List<EntryResponse> toResponses(CompanyId companyId, Collection<Entry> entries, Map<UUID, String> warnings) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<ProjectId, Project> projectById = projects.findByIds(
                entries.stream().map(Entry::getProjectId).distinct().toList()).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        List<TaskId> taskIds = entries.stream().map(Entry::getTaskId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<TaskId, Task> taskById = tasks.findByIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        Map<WeekId, TimesheetWeek> weekById = weeks.findByIds(
                entries.stream().map(Entry::getWeekId).distinct().toList()).stream()
                .collect(Collectors.toMap(TimesheetWeek::getId, Function.identity()));
        Map<UUID, EmployeeRef> employeeById = employees.findAll(companyId,
                entries.stream().map(Entry::getEmployeeId).distinct().toList());
        Map<UUID, WeekStatus> weekStatuses = weekById.values().stream()
                .collect(Collectors.toMap(w -> w.getId().getId(), TimesheetWeek::getStatus));
        Map<UUID, String> billing = billingSync.statuses(companyId, entries, weekStatuses);
        UUID actorId = access.actor(companyId).map(EmployeeRef::id).orElse(null);
        boolean costViewer = access.can(TimesheetPermissions.COST_VIEW);
        return entries.stream().map(e -> {
            boolean showCost = costViewer || e.getEmployeeId().equals(actorId);
            Project p = projectById.get(e.getProjectId());
            Task t = e.getTaskId() == null ? null : taskById.get(e.getTaskId());
            TimesheetWeek w = weekById.get(e.getWeekId());
            EmployeeRef emp = employeeById.get(e.getEmployeeId());
            boolean editable = w != null && w.isEditable() && access.canModifyFor(companyId, e.getEmployeeId());
            return new EntryResponse(e.getId().getId(), e.getEmployeeId(), emp == null ? null : emp.name(),
                    e.getWorkDate(), e.getMinutes(), e.getProjectId().getId(), p == null ? null : p.getName(),
                    p == null ? null : p.getCode(), e.getTaskId() == null ? null : e.getTaskId().getId(),
                    t == null ? null : t.getName(), e.getDescription(), e.isBillable(), e.getSource(),
                    e.getWeekId().getId(), w == null ? null : w.getStatus(), editable,
                    warnings == null ? null : warnings.get(e.getId().getId()),
                    showCost ? e.getCostRate() : null, showCost ? e.getCostAmount() : null,
                    showCost ? e.getCostCurrency() : null, showCost ? e.isCostMissing() : null,
                    billing.get(e.getId().getId()), e.getSaleLineId());
        }).toList();
    }

    EntryResponse toResponse(CompanyId companyId, Entry entry, String warning) {
        return toResponses(companyId, List.of(entry),
                warning == null ? Map.of() : Map.of(entry.getId().getId(), warning)).get(0);
    }
}
