package com.bradox.delin.inventory.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.inventory.domain.core.entity.StockLocation;
import com.bradox.delin.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.delin.inventory.domain.core.valueobject.WarehouseId;

import java.util.List;
import java.util.Optional;

public interface StockLocationRepository {
    StockLocation save(StockLocation location);
    Optional<StockLocation> findById(StockLocationId id);
    Optional<StockLocation> findByIdIncludingArchived(StockLocationId id);
    List<StockLocation> findByCompany(CompanyId companyId, boolean includeArchived);
    List<StockLocation> findByWarehouse(WarehouseId warehouseId, boolean includeArchived);
}
