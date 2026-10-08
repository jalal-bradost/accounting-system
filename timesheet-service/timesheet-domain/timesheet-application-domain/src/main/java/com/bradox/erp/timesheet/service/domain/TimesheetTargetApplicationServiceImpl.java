package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.service.domain.ports.input.TimesheetTargetApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
class TimesheetTargetApplicationServiceImpl implements TimesheetTargetApplicationService {

    private static final Map<String, String> PROJECT_NAMES = Map.of("repair.order", "Workshop");

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TimesheetAccess access;

    TimesheetTargetApplicationServiceImpl(ProjectRepository projects, TaskRepository tasks, TimesheetAccess access) {
        this.projects = projects;
        this.tasks = tasks;
        this.access = access;
    }

    @Override
    @Transactional
    public UUID ensureTask(CompanyId companyId, String model, UUID recordId, String reference, UUID partnerId) {
        var existing = tasks.findByRecord(companyId, model, recordId);
        if (existing.isPresent()) {
            return existing.get().getId().getId();
        }
        String key = "target:" + model;
        Project project = projects.findBySystemKey(companyId, key).orElseGet(() -> projects.createIfAbsent(Project.createLinked(
                new ProjectId(UUID.randomUUID()), companyId, PROJECT_NAMES.getOrDefault(model, model), model, key,
                access.clock().instant())));
        Task task = Task.create(new TaskId(UUID.randomUUID()), companyId, project.getId(), reference, null, Set.of(),
                null, null, access.clock().instant(), "system");
        task.linkRecord(model, recordId);
        return tasks.save(task).getId().getId();
    }

    @Override
    @Transactional
    public void closeTask(CompanyId companyId, String model, UUID recordId, boolean canceled) {
        tasks.findByRecord(companyId, model, recordId).ifPresent(t -> {
            t.changeStatus(canceled ? TaskStatus.CANCELED : TaskStatus.DONE, access.clock().instant());
            tasks.save(t);
        });
    }

    @Override
    @Transactional
    public void archiveTask(CompanyId companyId, String model, UUID recordId) {
        closeTask(companyId, model, recordId, true);
    }
}
