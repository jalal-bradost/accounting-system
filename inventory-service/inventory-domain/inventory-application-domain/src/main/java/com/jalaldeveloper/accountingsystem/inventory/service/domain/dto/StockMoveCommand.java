package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class StockMoveCommand {

    @NotNull private UUID productId;
    @NotNull private UUID uomId;
    @NotNull
    @Positive
    private BigDecimal demandQuantity;
    /** Optional unit cost override (e.g. PO price for incoming). */
    private BigDecimal unitCost;
    /** Optional link to {@code pur_purchase_order_line} for purchase receipts. */
    private UUID purchaseOrderLineId;
    /** Optional link to {@code sal_sales_order_line} for customer deliveries. */
    private UUID salesOrderLineId;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID v) { this.uomId = v; }
    public BigDecimal getDemandQuantity() { return demandQuantity; }
    public void setDemandQuantity(BigDecimal v) { this.demandQuantity = v; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal v) { this.unitCost = v; }
    public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
    public void setPurchaseOrderLineId(UUID v) { this.purchaseOrderLineId = v; }
    public UUID getSalesOrderLineId() { return salesOrderLineId; }
    public void setSalesOrderLineId(UUID v) { this.salesOrderLineId = v; }
}
