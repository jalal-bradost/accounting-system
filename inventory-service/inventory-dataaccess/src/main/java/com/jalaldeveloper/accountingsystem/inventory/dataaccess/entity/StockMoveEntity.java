package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inv_stock_move", indexes = {
        @Index(name = "ix_inv_move_picking", columnList = "picking_id"),
        @Index(name = "ix_inv_move_product", columnList = "product_id"),
        @Index(name = "ix_inv_move_state", columnList = "state"),
        @Index(name = "ix_inv_move_po_line", columnList = "purchase_order_line_id"),
        @Index(name = "ix_inv_move_so_line", columnList = "sales_order_line_id")
})
public class StockMoveEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inv_move_picking"))
    private StockPickingEntity picking;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "uom_id", nullable = false)
    private UUID uomId;

    @Column(name = "source_location_id", nullable = false)
    private UUID sourceLocationId;

    @Column(name = "destination_location_id", nullable = false)
    private UUID destinationLocationId;

    @Column(name = "demand_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal demandQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(name = "picked_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal pickedQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MoveState state;

    @Column(name = "purchase_order_line_id")
    private UUID purchaseOrderLineId;

    @Column(name = "sales_order_line_id")
    private UUID salesOrderLineId;

    public StockMoveEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public StockPickingEntity getPicking() { return picking; }
    public void setPicking(StockPickingEntity picking) { this.picking = picking; }
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
