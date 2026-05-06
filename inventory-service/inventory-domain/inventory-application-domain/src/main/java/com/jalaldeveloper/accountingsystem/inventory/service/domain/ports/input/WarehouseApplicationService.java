package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseResponse;
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
