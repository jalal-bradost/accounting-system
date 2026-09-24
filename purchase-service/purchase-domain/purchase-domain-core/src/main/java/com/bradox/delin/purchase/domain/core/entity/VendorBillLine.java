package com.bradox.delin.purchase.domain.core.entity;

import com.bradox.delin.domain.valueobject.DiscountType;

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
    /** Discount carried from the order. {@code discountPercent} is derived for reporting. */
    private DiscountType discountType = DiscountType.PERCENT;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO;
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
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = DiscountType.orPercent(discountType); }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
    }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent != null ? discountPercent : BigDecimal.ZERO;
    }
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
