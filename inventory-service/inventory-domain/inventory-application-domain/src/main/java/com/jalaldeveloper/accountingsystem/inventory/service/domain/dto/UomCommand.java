package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class UomCommand {
    private UUID companyId;
    @NotNull private UUID categoryId;
    @NotBlank private String name;
    @NotNull private UomType uomType;
    @NotNull private BigDecimal factor;
    private Integer rounding;

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
    public Integer getRounding() { return rounding; }
    public void setRounding(Integer v) { this.rounding = v; }
}
