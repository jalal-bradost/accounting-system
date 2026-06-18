package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductResponse {

    private UUID id;
    private UUID companyId;
    private String sku;
    private String name;
    private String barcode;
    private String description;
    private ProductType productType;
    private UUID categoryId;
    private UUID uomId;
    private UUID purchaseUomId;
    private BigDecimal standardCost;
    private BigDecimal listPrice;
    private boolean purchaseOk;
    private boolean saleOk;
    private ValuationMethod valuationMethodOverride;
    private UUID stockValuationAccountIdOverride;
    private UUID stockInputAccountIdOverride;
    private UUID stockOutputAccountIdOverride;
    private UUID cogsAccountIdOverride;
    private boolean active;
    private Instant archivedAt;
    private String imageUrl;
    private String imageContentType;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { this.barcode = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType v) { this.productType = v; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID v) { this.categoryId = v; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID v) { this.uomId = v; }
    public UUID getPurchaseUomId() { return purchaseUomId; }
    public void setPurchaseUomId(UUID v) { this.purchaseUomId = v; }
    public BigDecimal getStandardCost() { return standardCost; }
    public void setStandardCost(BigDecimal v) { this.standardCost = v; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal v) { this.listPrice = v; }
    public boolean isPurchaseOk() { return purchaseOk; }
    public void setPurchaseOk(boolean v) { this.purchaseOk = v; }
    public boolean isSaleOk() { return saleOk; }
    public void setSaleOk(boolean v) { this.saleOk = v; }
    public ValuationMethod getValuationMethodOverride() { return valuationMethodOverride; }
    public void setValuationMethodOverride(ValuationMethod v) { this.valuationMethodOverride = v; }
    public UUID getStockValuationAccountIdOverride() { return stockValuationAccountIdOverride; }
    public void setStockValuationAccountIdOverride(UUID v) { this.stockValuationAccountIdOverride = v; }
    public UUID getStockInputAccountIdOverride() { return stockInputAccountIdOverride; }
    public void setStockInputAccountIdOverride(UUID v) { this.stockInputAccountIdOverride = v; }
    public UUID getStockOutputAccountIdOverride() { return stockOutputAccountIdOverride; }
    public void setStockOutputAccountIdOverride(UUID v) { this.stockOutputAccountIdOverride = v; }
    public UUID getCogsAccountIdOverride() { return cogsAccountIdOverride; }
    public void setCogsAccountIdOverride(UUID v) { this.cogsAccountIdOverride = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant v) { this.archivedAt = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String v) { this.imageContentType = v; }
}
