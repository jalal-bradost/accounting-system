package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.platform.audit.AuditTrack;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditableModel;
import com.jalaldeveloper.accountingsystem.platform.audit.AuditingEntityListener;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inv_product_packaging", indexes = {
        @Index(name = "ix_inv_product_packaging_product", columnList = "product_id,active")
})
@EntityListeners(AuditingEntityListener.class)
@AuditableModel("inventory.product.packaging")
public class ProductPackagingEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 128)
    @AuditTrack
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    @AuditTrack
    private BigDecimal qty;

    @Column(name = "purchase_price", nullable = false, precision = 19, scale = 4)
    @AuditTrack
    private BigDecimal purchasePrice;

    @Column(name = "list_price", nullable = false, precision = 19, scale = 4)
    @AuditTrack
    private BigDecimal listPrice;

    @Column(length = 100)
    @AuditTrack
    private String barcode;

    @Column(length = 64)
    @AuditTrack
    private String sku;

    @Column(nullable = false)
    @AuditTrack
    private boolean active = true;

    @Column(name = "is_base", nullable = false)
    private boolean base;

    @Column(name = "packaged_product_id")
    private UUID packagedProductId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductPackagingEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isBase() { return base; }
    public void setBase(boolean base) { this.base = base; }
    public UUID getPackagedProductId() { return packagedProductId; }
    public void setPackagedProductId(UUID packagedProductId) { this.packagedProductId = packagedProductId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
