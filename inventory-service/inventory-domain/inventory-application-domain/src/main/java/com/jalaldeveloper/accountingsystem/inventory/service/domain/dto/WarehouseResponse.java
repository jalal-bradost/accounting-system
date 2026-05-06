package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import java.util.UUID;

public class WarehouseResponse {
    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private UUID stockLocationId;
    private UUID inputLocationId;
    private UUID outputLocationId;
    private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getStockLocationId() { return stockLocationId; }
    public void setStockLocationId(UUID v) { this.stockLocationId = v; }
    public UUID getInputLocationId() { return inputLocationId; }
    public void setInputLocationId(UUID v) { this.inputLocationId = v; }
    public UUID getOutputLocationId() { return outputLocationId; }
    public void setOutputLocationId(UUID v) { this.outputLocationId = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}
