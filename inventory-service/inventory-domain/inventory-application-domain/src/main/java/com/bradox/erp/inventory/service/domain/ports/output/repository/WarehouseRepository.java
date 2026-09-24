package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.Warehouse;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(WarehouseId id);
    Optional<Warehouse> findByIdIncludingArchived(WarehouseId id);
    List<Warehouse> findByCompany(CompanyId companyId, boolean includeArchived);
}
