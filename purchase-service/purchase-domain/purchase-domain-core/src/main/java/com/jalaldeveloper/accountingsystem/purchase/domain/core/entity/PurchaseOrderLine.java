package com.jalaldeveloper.accountingsystem.purchase.domain.core.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderLine {

    private UUID id;
    private int sequence;
    private UUID productId;
    private String name;
    private UUID uomId;
    private UUID warehouseId;
    private BigDecimal qtyOrdered;
    private BigDecimal qtyReceived;
    private BigDecimal qtyInvoiced;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private LocalDate expectedDate;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PurchaseOrderLineTax> taxes = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public List<PurchaseOrderLineTax> getTaxes() { return taxes; }
    public void setTaxes(List<PurchaseOrderLineTax> taxes) { this.taxes = taxes != null ? taxes : new ArrayList<>(); }
}
