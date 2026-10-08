package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.TaskEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.TaskJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskJpaRepository tasks;
    private final TimesheetDataAccessMapper mapper;

    public TaskRepositoryImpl(TaskJpaRepository tasks, TimesheetDataAccessMapper mapper) {
        this.tasks = tasks;
        this.mapper = mapper;
    }

    @Override
    public Optional<Task> find(TaskId id) {
        return tasks.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<Task> findByIds(Collection<TaskId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return tasks.findAllById(ids.stream().map(TaskId::getId).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Task> findByProject(CompanyId companyId, ProjectId projectId) {
        return tasks.findByCompanyIdAndProjectId(companyId.getId(), projectId.getId()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Task> findOpenByProjects(CompanyId companyId, Collection<ProjectId> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return tasks.findByCompanyIdAndStatusInAndProjectIdIn(companyId.getId(),
                List.of(TaskStatus.TODO.name(), TaskStatus.IN_PROGRESS.name()),
                projectIds.stream().map(ProjectId::getId).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Task> findByRecord(CompanyId companyId, String recordModel, java.util.UUID recordId) {
        return tasks.findByCompanyIdAndRecordModelAndRecordId(companyId.getId(), recordModel, recordId).map(mapper::toDomain);
    }

    @Override
    public Task save(Task task) {
        TaskEntity entity = tasks.findById(task.getId().getId()).orElseGet(TaskEntity::new);
        mapper.apply(task, entity);
        return mapper.toDomain(tasks.saveAndFlush(entity));
    }
}
