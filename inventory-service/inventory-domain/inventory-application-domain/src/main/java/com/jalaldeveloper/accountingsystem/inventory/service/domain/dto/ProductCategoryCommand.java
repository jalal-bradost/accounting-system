package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class ProductCategoryCommand {
    private UUID companyId;
    @NotBlank private String name;
    private UUID parentId;
    private ValuationMethod valuationMethod;
    private UUID stockValuationAccountId;
    private UUID stockInputAccountId;
    private UUID stockOutputAccountId;
    private UUID cogsAccountId;

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
}
