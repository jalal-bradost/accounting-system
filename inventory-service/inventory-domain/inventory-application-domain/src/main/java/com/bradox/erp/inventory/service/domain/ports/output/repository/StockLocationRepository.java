package com.bradox.erp.inventory.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.domain.core.entity.StockLocation;
import com.bradox.erp.inventory.domain.core.valueobject.StockLocationId;
import com.bradox.erp.inventory.domain.core.valueobject.WarehouseId;

import java.util.List;
import java.util.Optional;

public interface StockLocationRepository {
    StockLocation save(StockLocation location);
    Optional<StockLocation> findById(StockLocationId id);
    Optional<StockLocation> findByIdIncludingArchived(StockLocationId id);
    List<StockLocation> findByCompany(CompanyId companyId, boolean includeArchived);
    List<StockLocation> findByWarehouse(WarehouseId warehouseId, boolean includeArchived);
}
