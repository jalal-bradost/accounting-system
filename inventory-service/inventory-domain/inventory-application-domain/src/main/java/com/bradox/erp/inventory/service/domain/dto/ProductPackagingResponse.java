package com.bradox.erp.inventory.service.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductPackagingResponse {

    private UUID id;
    private UUID companyId;
    private UUID productId;
    private String name;
    private BigDecimal qty;
    private BigDecimal purchasePrice;
    private BigDecimal listPrice;
    private String barcode;
    private String sku;
    private boolean active;
    private boolean base;
    private UUID packagedProductId;
    private Instant createdAt;
    private Instant updatedAt;

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
