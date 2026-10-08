package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.service.domain.dto.PackageCommand;

import java.util.List;
import java.util.UUID;

public interface PackageApplicationService {

    List<ServicePackage> list(CompanyId companyId, String query, boolean includeArchived);

    ServicePackage get(CompanyId companyId, UUID id);

    ServicePackage save(CompanyId companyId, UUID id, PackageCommand command);

    ServicePackage setActive(CompanyId companyId, UUID id, boolean active);
}
