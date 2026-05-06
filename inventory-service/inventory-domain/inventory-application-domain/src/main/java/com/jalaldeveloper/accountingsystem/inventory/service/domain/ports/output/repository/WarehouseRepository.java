package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.Warehouse;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(WarehouseId id);
    Optional<Warehouse> findByIdIncludingArchived(WarehouseId id);
    List<Warehouse> findByCompany(CompanyId companyId, boolean includeArchived);
}
