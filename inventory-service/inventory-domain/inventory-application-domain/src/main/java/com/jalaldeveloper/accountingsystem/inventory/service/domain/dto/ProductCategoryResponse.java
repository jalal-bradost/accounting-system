package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.util.UUID;

public class ProductCategoryResponse {
    private UUID id;
    private UUID companyId;
    private String name;
    private UUID parentId;
    private ValuationMethod valuationMethod;
    private UUID stockValuationAccountId;
    private UUID stockInputAccountId;
    private UUID stockOutputAccountId;
    private UUID cogsAccountId;
    private boolean active;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID v) { this.parentId = v; }
    public ValuationMethod getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(ValuationMethod v) { this.valuationMethod = v; }
    public UUID getStockValuationAccountId() { return stockValuationAccountId; }
    public void setStockValuationAccountId(UUID v) { this.stockValuationAccountId = v; }
    public UUID getStockInputAccountId() { return stockInputAccountId; }
    public void setStockInputAccountId(UUID v) { this.stockInputAccountId = v; }
    public UUID getStockOutputAccountId() { return stockOutputAccountId; }
    public void setStockOutputAccountId(UUID v) { this.stockOutputAccountId = v; }
    public UUID getCogsAccountId() { return cogsAccountId; }
    public void setCogsAccountId(UUID v) { this.cogsAccountId = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}
