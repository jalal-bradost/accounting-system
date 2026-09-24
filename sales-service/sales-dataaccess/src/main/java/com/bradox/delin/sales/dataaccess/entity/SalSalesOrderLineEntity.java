package com.bradox.delin.sales.dataaccess.entity;

import com.bradox.delin.domain.valueobject.DiscountType;
import com.bradox.delin.sales.domain.core.SalInvoicePolicy;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sal_sales_order_line", indexes = {
        @Index(name = "ix_sal_sol_order", columnList = "sales_order_id")
})
public class SalSalesOrderLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sal_sol_order"))
    private SalSalesOrderEntity salesOrder;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "uom_id", nullable = false)
    private UUID uomId;

    @Column(name = "packaging_id")
    private UUID packagingId;

    @Column(name = "packaging_name", length = 128)
    private String packagingName;

    @Column(name = "qty_per_package", precision = 19, scale = 4)
    private BigDecimal qtyPerPackage;

    @Column(name = "qty_ordered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_delivered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyDelivered;

    @Column(name = "qty_invoiced", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyInvoiced;

    @Column(name = "qty_cogs_cleared", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyCogsCleared = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 16)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_percent", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_policy", length = 32)
    private SalInvoicePolicy invoicePolicy;

    @Column(name = "revenue_account_id")
    private UUID revenueAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<SalSalesOrderLineTaxEntity> taxes = new ArrayList<>();

    public SalSalesOrderLineEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SalSalesOrderEntity getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalSalesOrderEntity salesOrder) { this.salesOrder = salesOrder; }
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
    public List<SalSalesOrderLineTaxEntity> getTaxes() { return taxes; }
    public void setTaxes(List<SalSalesOrderLineTaxEntity> taxes) {
        this.taxes = taxes != null ? taxes : new ArrayList<>();
    }
}
