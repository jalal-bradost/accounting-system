package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inv_stock_picking", indexes = {
        @Index(name = "ix_inv_pick_company", columnList = "company_id"),
        @Index(name = "ix_inv_pick_company_state", columnList = "company_id,state"),
        @Index(name = "ix_inv_pick_company_type", columnList = "company_id,picking_type"),
        @Index(name = "ix_inv_pick_partner", columnList = "partner_id"),
        @Index(name = "ix_inv_pick_purchase_order", columnList = "purchase_order_id")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.stock.picking")
public class StockPickingEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "picking_type", nullable = false, length = 20)
    @AuditTrack(name = "pickingType")
    private PickingType pickingType;

    @Column(length = 100)
    @AuditTrack
    private String reference;

    @Column(name = "source_location_id", nullable = false)
    private UUID sourceLocationId;

    @Column(name = "destination_location_id", nullable = false)
    private UUID destinationLocationId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(length = 255)
    private String origin;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "validated_by", length = 255)
    private String validatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @AuditTrack
    private PickingState state;

    @Column(name = "backorder_of")
    private UUID backorderOf;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @OneToMany(mappedBy = "picking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockMoveEntity> moves = new ArrayList<>();

    public StockPickingEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public List<StockMoveEntity> getMoves() { return moves; }
    public void setMoves(List<StockMoveEntity> v) { this.moves = v != null ? v : new ArrayList<>(); }
}
