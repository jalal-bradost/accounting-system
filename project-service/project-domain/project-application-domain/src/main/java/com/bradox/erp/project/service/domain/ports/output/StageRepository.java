package com.bradox.erp.project.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.domain.core.model.Stage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StageRepository {

    /** In board order. */
    List<Stage> findByProject(CompanyId companyId, UUID projectId);

    Optional<Stage> find(CompanyId companyId, UUID id);

    Stage save(Stage stage);

    void delete(Stage stage);

    void deleteByProject(CompanyId companyId, UUID projectId);
}
