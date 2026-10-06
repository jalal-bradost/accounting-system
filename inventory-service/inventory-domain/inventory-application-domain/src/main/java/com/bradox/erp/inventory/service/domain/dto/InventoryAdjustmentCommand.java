package com.bradox.erp.inventory.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adjusts on-hand quantity at a single (product, location) to a target value. Posts a
 * single virtual move (counterparty: INVENTORY_LOSS) and a corresponding SVL/JE.
 *
 * <p>When {@code openingBalance} is true, valuation journals against Opening Balance Equity
 * instead of COGS so go-live stock does not distort P&amp;L.
 */
public class InventoryAdjustmentCommand {
    public static final String OPENING_STOCK_REASON = "Opening stock";

    private UUID companyId;
    @NotNull private UUID productId;
    @NotNull private UUID locationId;
    @NotNull private BigDecimal targetQuantity;
    private String reason;
    /** When true, credit/debit Opening Balance Equity instead of COGS. */
    private boolean openingBalance;
    /** Optional unit cost override (opening stock / inventory). */
    private BigDecimal unitCost;

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
    public boolean isOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(boolean openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
