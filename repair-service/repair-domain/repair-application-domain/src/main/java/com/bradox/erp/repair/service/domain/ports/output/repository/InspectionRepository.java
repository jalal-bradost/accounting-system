package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.Finding;
import com.bradox.erp.repair.domain.core.model.Inspection;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository {

    List<InspectionTemplate> templates(CompanyId companyId, boolean includeInactive);

    Optional<InspectionTemplate> findTemplate(CompanyId companyId, UUID id);

    InspectionTemplate save(InspectionTemplate template);

    Optional<Inspection> findByOrder(CompanyId companyId, UUID orderId);

    Inspection save(Inspection inspection);

    List<Finding> findings(CompanyId companyId, UUID orderId);

    Optional<Finding> findFinding(CompanyId companyId, UUID id);

    Finding save(Finding finding);
}
