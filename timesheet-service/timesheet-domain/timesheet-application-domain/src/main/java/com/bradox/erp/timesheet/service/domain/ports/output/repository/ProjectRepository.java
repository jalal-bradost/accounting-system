package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Optional<Project> find(ProjectId id);

    List<Project> findByIds(Collection<ProjectId> ids);

    List<Project> findAll(CompanyId companyId, boolean includeArchived);

    Optional<Project> findBySystemKey(CompanyId companyId, String systemKey);

    /** Case-insensitive; {@code excludeId} lets an update keep its own code. */
    boolean codeTaken(CompanyId companyId, String normalizedCode, ProjectId excludeId);

    Project save(Project project);

    /**
     * Inserts a system-keyed project unless one with the same (company, system key) already exists, including
     * one committed concurrently by another request. Returns the project that ends up stored.
     */
    Project createIfAbsent(Project project);
}
