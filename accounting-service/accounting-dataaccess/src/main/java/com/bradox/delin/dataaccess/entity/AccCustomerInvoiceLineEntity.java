package com.bradox.delin.dataaccess.entity;

import com.bradox.delin.domain.valueobject.DiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "acc_customer_invoice_line", indexes = {
        @Index(name = "ix_acc_cil_invoice", columnList = "customer_invoice_id")
})
public class AccCustomerInvoiceLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_invoice_id", nullable = false, foreignKey = @ForeignKey(name = "fk_acc_cil_invoice"))
    private AccCustomerInvoiceEntity invoice;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal qty;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "revenue_account_id", nullable = false)
    private UUID revenueAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 16)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_percent", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountPercent;

    @Column(name = "sales_order_line_id")
    private UUID salesOrderLineId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<AccCustomerInvoiceLineTaxEntity> taxSnapshots = new ArrayList<>();

    public AccCustomerInvoiceLineEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AccCustomerInvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(AccCustomerInvoiceEntity invoice) { this.invoice = invoice; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = DiscountType.orPercent(discountType); }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
    }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public UUID getSalesOrderLineId() { return salesOrderLineId; }
    public void setSalesOrderLineId(UUID salesOrderLineId) { this.salesOrderLineId = salesOrderLineId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<AccCustomerInvoiceLineTaxEntity> getTaxSnapshots() { return taxSnapshots; }
    public void setTaxSnapshots(List<AccCustomerInvoiceLineTaxEntity> taxSnapshots) {
        this.taxSnapshots = taxSnapshots != null ? taxSnapshots : new ArrayList<>();
    }
}
