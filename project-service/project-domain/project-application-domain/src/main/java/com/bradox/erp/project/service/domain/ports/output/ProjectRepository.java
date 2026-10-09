package com.bradox.erp.project.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.domain.core.model.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    List<Project> findAll(CompanyId companyId);

    Optional<Project> find(CompanyId companyId, UUID id);

    Project save(Project project);

    void delete(Project project);
}
