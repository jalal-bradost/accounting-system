package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.StockLocationResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseCommand;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.dto.WarehouseResponse;
import com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.input.WarehouseApplicationService;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/inventory", produces = "application/json")
public class WarehouseController {

    private final WarehouseApplicationService service;

    public WarehouseController(WarehouseApplicationService service) {
        this.service = service;
    }

    @PostMapping("/warehouses")
    @RequiresPermission("inventory.warehouse.write")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseCommand cmd) {
        return ResponseEntity.ok(service.createWarehouse(cmd));
    }

    @GetMapping("/warehouses/{id}")
    @RequiresPermission("inventory.warehouse.read")
    public ResponseEntity<WarehouseResponse> getWarehouse(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getWarehouse(id));
    }

    @PutMapping("/warehouses/{id}")
    @RequiresPermission("inventory.warehouse.write")
    public ResponseEntity<WarehouseResponse> updateWarehouse(@PathVariable UUID id,
                                                              @Valid @RequestBody WarehouseCommand cmd) {
        return ResponseEntity.ok(service.updateWarehouse(id, cmd));
    }

    @GetMapping("/warehouses")
    @RequiresPermission("inventory.warehouse.read")
    public ResponseEntity<List<WarehouseResponse>> listWarehouses(@CurrentCompany CompanyId companyId,
                                                                   @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(service.listWarehouses(companyId, includeArchived));
    }

    @PostMapping("/stock-locations")
    @RequiresPermission("inventory.warehouse.write")
    public ResponseEntity<StockLocationResponse> createLocation(@Valid @RequestBody StockLocationCommand cmd) {
        return ResponseEntity.ok(service.createLocation(cmd));
    }

    @GetMapping("/stock-locations/{id}")
    @RequiresPermission("inventory.warehouse.read")
    public ResponseEntity<StockLocationResponse> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getLocation(id));
    }

    @PutMapping("/stock-locations/{id}")
    @RequiresPermission("inventory.warehouse.write")
    public ResponseEntity<StockLocationResponse> updateLocation(@PathVariable UUID id,
                                                                 @Valid @RequestBody StockLocationCommand cmd) {
        return ResponseEntity.ok(service.updateLocation(id, cmd));
    }

    @GetMapping("/stock-locations")
    @RequiresPermission("inventory.warehouse.read")
    public ResponseEntity<List<StockLocationResponse>> listLocations(@CurrentCompany CompanyId companyId,
                                                                      @RequestParam(defaultValue = "false") boolean includeArchived,
                                                                      @RequestParam(required = false) UUID warehouseId) {
        if (warehouseId != null) {
            return ResponseEntity.ok(service.listLocationsByWarehouse(warehouseId, includeArchived));
        }
        return ResponseEntity.ok(service.listLocations(companyId, includeArchived));
    }
}
