package com.bradox.erp.project.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.domain.core.model.Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {

    /** In board order (stage, then sequence). */
    List<Task> findByProject(CompanyId companyId, UUID projectId);

    Optional<Task> find(CompanyId companyId, UUID id);

    /** Number of tasks per project id. */
    Map<UUID, Long> countByProject(CompanyId companyId);

    boolean existsInStage(CompanyId companyId, UUID stageId);

    Task save(Task task);

    void delete(Task task);

    void deleteByProject(CompanyId companyId, UUID projectId);
}
