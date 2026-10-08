package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Optional<Task> find(TaskId id);

    List<Task> findByIds(Collection<TaskId> ids);

    List<Task> findByProject(CompanyId companyId, ProjectId projectId);

    /** Tasks the picker may offer: TODO or IN_PROGRESS on active projects. */
    List<Task> findOpenByProjects(CompanyId companyId, Collection<ProjectId> projectIds);

    Optional<Task> findByRecord(CompanyId companyId, String recordModel, java.util.UUID recordId);

    Task save(Task task);
}
