package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pur_vendor_bill_line", indexes = {
        @Index(name = "ix_pur_vbl_bill", columnList = "vendor_bill_id"),
        @Index(name = "ix_pur_vbl_pol", columnList = "purchase_order_line_id")
})
public class PurVendorBillLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_bill_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pur_vbl_bill"))
    private PurVendorBillEntity vendorBill;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "purchase_order_line_id")
    private UUID purchaseOrderLineId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "uom_id", nullable = false)
    private UUID uomId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal qty;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurVendorBillLineTaxEntity> taxSnapshots = new ArrayList<>();

    public PurVendorBillLineEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PurVendorBillEntity getVendorBill() { return vendorBill; }
    public void setVendorBill(PurVendorBillEntity vendorBill) { this.vendorBill = vendorBill; }
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
    public List<PurVendorBillLineTaxEntity> getTaxSnapshots() { return taxSnapshots; }
    public void setTaxSnapshots(List<PurVendorBillLineTaxEntity> taxSnapshots) {
        this.taxSnapshots = taxSnapshots != null ? taxSnapshots : new ArrayList<>();
    }
}
