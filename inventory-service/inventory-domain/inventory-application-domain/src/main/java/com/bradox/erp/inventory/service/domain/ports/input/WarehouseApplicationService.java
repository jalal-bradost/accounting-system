package com.bradox.erp.inventory.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.inventory.service.domain.dto.StockLocationCommand;
import com.bradox.erp.inventory.service.domain.dto.StockLocationResponse;
import com.bradox.erp.inventory.service.domain.dto.WarehouseCommand;
import com.bradox.erp.inventory.service.domain.dto.WarehouseResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface WarehouseApplicationService {

    WarehouseResponse createWarehouse(@Valid WarehouseCommand command);
    WarehouseResponse updateWarehouse(UUID warehouseId, @Valid WarehouseCommand command);
    WarehouseResponse getWarehouse(UUID warehouseId);
    List<WarehouseResponse> listWarehouses(CompanyId companyId, boolean includeArchived);

    StockLocationResponse createLocation(@Valid StockLocationCommand command);
    StockLocationResponse updateLocation(UUID locationId, @Valid StockLocationCommand command);
    StockLocationResponse getLocation(UUID locationId);
    List<StockLocationResponse> listLocations(CompanyId companyId, boolean includeArchived);
    List<StockLocationResponse> listLocationsByWarehouse(UUID warehouseId, boolean includeArchived);
}
