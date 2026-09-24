package com.bradox.delin.sales.domain.core.entity;

import com.bradox.delin.domain.valueobject.DiscountType;
import com.bradox.delin.sales.domain.core.SalInvoicePolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesOrderLine {

    private UUID id;
    private int sequence;
    private UUID productId;
    private String name;
    private UUID uomId;
    private UUID packagingId;
    private String packagingName;
    private BigDecimal qtyPerPackage;
    private BigDecimal qtyOrdered;
    private BigDecimal qtyDelivered;
    private BigDecimal qtyInvoiced;
    /** Qty for which Stock Output → COGS has already been posted (Anglo-Saxon). */
    private BigDecimal qtyCogsCleared;
    private BigDecimal unitPrice;
    /** Discount as entered. {@code discountPercent} is the derived percentage for reporting. */
    private DiscountType discountType = DiscountType.PERCENT;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal discountPercent;
    private SalInvoicePolicy invoicePolicy;
    private UUID revenueAccountId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SalesOrderLineTax> taxes = new ArrayList<>();

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
    public UUID getPackagingId() { return packagingId; }
    public void setPackagingId(UUID packagingId) { this.packagingId = packagingId; }
    public String getPackagingName() { return packagingName; }
    public void setPackagingName(String packagingName) { this.packagingName = packagingName; }
    public BigDecimal getQtyPerPackage() { return qtyPerPackage; }
    public void setQtyPerPackage(BigDecimal qtyPerPackage) { this.qtyPerPackage = qtyPerPackage; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyDelivered() { return qtyDelivered; }
    public void setQtyDelivered(BigDecimal qtyDelivered) { this.qtyDelivered = qtyDelivered; }
    public BigDecimal getQtyInvoiced() { return qtyInvoiced; }
    public void setQtyInvoiced(BigDecimal qtyInvoiced) { this.qtyInvoiced = qtyInvoiced; }
    public BigDecimal getQtyCogsCleared() { return qtyCogsCleared; }
    public void setQtyCogsCleared(BigDecimal qtyCogsCleared) {
        this.qtyCogsCleared = qtyCogsCleared != null ? qtyCogsCleared : BigDecimal.ZERO;
    }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = DiscountType.orPercent(discountType); }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
    }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public SalInvoicePolicy getInvoicePolicy() { return invoicePolicy; }
    public void setInvoicePolicy(SalInvoicePolicy invoicePolicy) { this.invoicePolicy = invoicePolicy; }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<SalesOrderLineTax> getTaxes() { return taxes; }
    public void setTaxes(List<SalesOrderLineTax> taxes) {
        this.taxes = taxes != null ? taxes : new ArrayList<>();
    }
}
