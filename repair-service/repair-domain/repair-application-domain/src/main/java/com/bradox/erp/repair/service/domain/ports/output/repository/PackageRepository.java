package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.ServicePackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageRepository {

    List<ServicePackage> list(CompanyId companyId, String query, boolean includeArchived);

    Optional<ServicePackage> find(CompanyId companyId, UUID id);

    ServicePackage save(ServicePackage servicePackage);
}
