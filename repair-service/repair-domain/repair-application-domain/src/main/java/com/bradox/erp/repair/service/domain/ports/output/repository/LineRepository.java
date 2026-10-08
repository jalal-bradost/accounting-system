package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.RepairLine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LineRepository {

    List<RepairLine> listByOrder(CompanyId companyId, UUID orderId);

    Optional<RepairLine> find(CompanyId companyId, UUID id);

    RepairLine save(RepairLine line);

    void delete(CompanyId companyId, UUID id);
}
