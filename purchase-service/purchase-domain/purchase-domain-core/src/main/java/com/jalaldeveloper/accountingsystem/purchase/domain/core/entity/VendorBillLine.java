package com.jalaldeveloper.accountingsystem.purchase.domain.core.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VendorBillLine {

    private UUID id;
    private int sequence;
    private UUID purchaseOrderLineId;
    private UUID productId;
    private String name;
    private UUID uomId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private UUID accountId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<VendorBillLineTax> taxSnapshots = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
    public void setPurchaseOrderLineId(UUID purchaseOrderLineId) { this.purchaseOrderLineId = purchaseOrderLineId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<VendorBillLineTax> getTaxSnapshots() { return taxSnapshots; }
    public void setTaxSnapshots(List<VendorBillLineTax> taxSnapshots) {
        this.taxSnapshots = taxSnapshots != null ? taxSnapshots : new ArrayList<>();
    }
}
