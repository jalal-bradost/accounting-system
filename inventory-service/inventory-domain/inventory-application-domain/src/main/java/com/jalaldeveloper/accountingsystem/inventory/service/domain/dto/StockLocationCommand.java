package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class StockLocationCommand {
    private UUID companyId;
    @NotBlank private String code;
    @NotBlank private String name;
    @NotNull private LocationType locationType;
    private UUID parentId;
    private UUID warehouseId;
    private boolean allowNegativeStock;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocationType getLocationType() { return locationType; }
    public void setLocationType(LocationType v) { this.locationType = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID v) { this.warehouseId = v; }
    public boolean isAllowNegativeStock() { return allowNegativeStock; }
    public void setAllowNegativeStock(boolean v) { this.allowNegativeStock = v; }
}
