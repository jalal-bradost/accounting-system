package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomType;

import java.math.BigDecimal;
import java.util.UUID;

public class UomResponse {
    private UUID id;
    private UUID companyId;
    private UUID categoryId;
    private String name;
    private UomType uomType;
    private BigDecimal factor;
    private int rounding;
    private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID v) { this.categoryId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UomType getUomType() { return uomType; }
    public void setUomType(UomType v) { this.uomType = v; }
    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal v) { this.factor = v; }
    public int getRounding() { return rounding; }
    public void setRounding(int v) { this.rounding = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}
