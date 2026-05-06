package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderLineResponse {
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
    private List<UUID> taxIds;
    /** {@code STOCKABLE}, {@code CONSUMABLE}, {@code SERVICE} — from catalog; drives vendor-bill eligibility. */
    private String productType;

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
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
}
