package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateStockPickingCommand {
    private UUID companyId;
    @NotNull private UUID warehouseId;
    @NotNull private PickingType pickingType;
    private String reference;
    @NotNull private UUID sourceLocationId;
    @NotNull private UUID destinationLocationId;
    private UUID partnerId;
    private String origin;
    private Instant scheduledAt;
    /** Optional link to {@code pur_purchase_order} for incoming pickings created from a PO. */
    private UUID purchaseOrderId;
    /** Optional link to {@code sal_sales_order} for outgoing pickings created from a SO. */
    private UUID salesOrderId;
    @NotEmpty @Valid private List<StockMoveCommand> moves;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID v) { this.warehouseId = v; }
    public PickingType getPickingType() { return pickingType; }
    public void setPickingType(PickingType v) { this.pickingType = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public UUID getSourceLocationId() { return sourceLocationId; }
    public void setSourceLocationId(UUID v) { this.sourceLocationId = v; }
    public UUID getDestinationLocationId() { return destinationLocationId; }
    public void setDestinationLocationId(UUID v) { this.destinationLocationId = v; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID v) { this.partnerId = v; }
    public String getOrigin() { return origin; }
    public void setOrigin(String v) { this.origin = v; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant v) { this.scheduledAt = v; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(UUID v) { this.purchaseOrderId = v; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID v) { this.salesOrderId = v; }
    public List<StockMoveCommand> getMoves() { return moves; }
    public void setMoves(List<StockMoveCommand> v) { this.moves = v; }
}
