package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adjusts on-hand quantity at a single (product, location) to a target value. Posts a
 * single virtual move (counterparty: INVENTORY_LOSS) and a corresponding SVL/JE.
 */
public class InventoryAdjustmentCommand {
    private UUID companyId;
    @NotNull private UUID productId;
    @NotNull private UUID locationId;
    @NotNull private BigDecimal targetQuantity;
    private String reason;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID v) { this.locationId = v; }
    public BigDecimal getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(BigDecimal v) { this.targetQuantity = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
