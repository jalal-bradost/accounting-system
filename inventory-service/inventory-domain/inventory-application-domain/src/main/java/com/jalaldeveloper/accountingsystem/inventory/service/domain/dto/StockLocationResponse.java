package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.LocationType;

import java.util.UUID;

public class StockLocationResponse {
    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private LocationType locationType;
    private UUID parentId;
    private UUID warehouseId;
    private boolean allowNegativeStock;
    private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
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
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}
