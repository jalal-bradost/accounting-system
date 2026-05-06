package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class StockPickingResponse {
    private UUID id;
    private UUID companyId;
    private UUID warehouseId;
    private PickingType pickingType;
    private String reference;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private UUID partnerId;
    private String origin;
    private Instant scheduledAt;
    private Instant validatedAt;
    private String validatedBy;
    private PickingState state;
    private UUID backorderOf;
    private UUID purchaseOrderId;
    private UUID salesOrderId;
    private List<MoveResponse> moves;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
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
    public Instant getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Instant v) { this.validatedAt = v; }
    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String v) { this.validatedBy = v; }
    public PickingState getState() { return state; }
    public void setState(PickingState v) { this.state = v; }
    public UUID getBackorderOf() { return backorderOf; }
    public void setBackorderOf(UUID v) { this.backorderOf = v; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(UUID v) { this.purchaseOrderId = v; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID v) { this.salesOrderId = v; }
    public List<MoveResponse> getMoves() { return moves; }
    public void setMoves(List<MoveResponse> v) { this.moves = v; }

    public static class MoveResponse {
        private UUID id;
        private UUID productId;
        private UUID uomId;
        private UUID sourceLocationId;
        private UUID destinationLocationId;
        private BigDecimal demandQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal pickedQuantity;
        private BigDecimal unitCost;
        private MoveState state;
        private UUID purchaseOrderLineId;
        private UUID salesOrderLineId;

        public UUID getId() { return id; }
        public void setId(UUID v) { this.id = v; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID v) { this.productId = v; }
        public UUID getUomId() { return uomId; }
        public void setUomId(UUID v) { this.uomId = v; }
        public UUID getSourceLocationId() { return sourceLocationId; }
        public void setSourceLocationId(UUID v) { this.sourceLocationId = v; }
        public UUID getDestinationLocationId() { return destinationLocationId; }
        public void setDestinationLocationId(UUID v) { this.destinationLocationId = v; }
        public BigDecimal getDemandQuantity() { return demandQuantity; }
        public void setDemandQuantity(BigDecimal v) { this.demandQuantity = v; }
        public BigDecimal getReservedQuantity() { return reservedQuantity; }
        public void setReservedQuantity(BigDecimal v) { this.reservedQuantity = v; }
        public BigDecimal getPickedQuantity() { return pickedQuantity; }
        public void setPickedQuantity(BigDecimal v) { this.pickedQuantity = v; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal v) { this.unitCost = v; }
        public MoveState getState() { return state; }
        public void setState(MoveState v) { this.state = v; }
        public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
        public void setPurchaseOrderLineId(UUID v) { this.purchaseOrderLineId = v; }
        public UUID getSalesOrderLineId() { return salesOrderLineId; }
        public void setSalesOrderLineId(UUID v) { this.salesOrderLineId = v; }
    }
}
