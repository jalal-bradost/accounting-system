package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pur_purchase_order_line", indexes = {
        @Index(name = "ix_pur_pol_order", columnList = "purchase_order_id")
})
public class PurPurchaseOrderLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pur_pol_order"))
    private PurPurchaseOrderEntity purchaseOrder;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "uom_id", nullable = false)
    private UUID uomId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "qty_ordered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_received", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyReceived;

    @Column(name = "qty_invoiced", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyInvoiced;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountPercent;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<PurPurchaseOrderLineTaxEntity> taxes = new ArrayList<>();

    public PurPurchaseOrderLineEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PurPurchaseOrderEntity getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurPurchaseOrderEntity purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyReceived() { return qtyReceived; }
    public void setQtyReceived(BigDecimal qtyReceived) { this.qtyReceived = qtyReceived; }
    public BigDecimal getQtyInvoiced() { return qtyInvoiced; }
    public void setQtyInvoiced(BigDecimal qtyInvoiced) { this.qtyInvoiced = qtyInvoiced; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<PurPurchaseOrderLineTaxEntity> getTaxes() { return taxes; }
    public void setTaxes(List<PurPurchaseOrderLineTaxEntity> taxes) { this.taxes = taxes != null ? taxes : new ArrayList<>(); }
}
