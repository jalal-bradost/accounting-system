package com.jalaldeveloper.accountingsystem.sales.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
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

    @Column(name = "qty_ordered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_delivered", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyDelivered;

    @Column(name = "qty_invoiced", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyInvoiced;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

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
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyDelivered() { return qtyDelivered; }
    public void setQtyDelivered(BigDecimal qtyDelivered) { this.qtyDelivered = qtyDelivered; }
    public BigDecimal getQtyInvoiced() { return qtyInvoiced; }
    public void setQtyInvoiced(BigDecimal qtyInvoiced) { this.qtyInvoiced = qtyInvoiced; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
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
