package com.bradox.erp.repair.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.RepairOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairOrderRepository {

    /** Newest first. */
    List<RepairOrder> findAll(CompanyId companyId);

    Optional<RepairOrder> find(CompanyId companyId, UUID id);

    RepairOrder save(RepairOrder order);
}
