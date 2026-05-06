package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.util.UUID;

/** Partial update; null fields are ignored. */
public class UpdateProductCommand {

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
    private Boolean purchaseOk;
    private Boolean saleOk;
    private ValuationMethod valuationMethodOverride;
    private Boolean valuationMethodOverrideReset;
    private UUID stockValuationAccountIdOverride;
    private UUID stockInputAccountIdOverride;
    private UUID stockOutputAccountIdOverride;
    private UUID cogsAccountIdOverride;

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
    public Boolean getPurchaseOk() { return purchaseOk; }
    public void setPurchaseOk(Boolean v) { this.purchaseOk = v; }
    public Boolean getSaleOk() { return saleOk; }
    public void setSaleOk(Boolean v) { this.saleOk = v; }
    public ValuationMethod getValuationMethodOverride() { return valuationMethodOverride; }
    public void setValuationMethodOverride(ValuationMethod v) { this.valuationMethodOverride = v; }
    public Boolean getValuationMethodOverrideReset() { return valuationMethodOverrideReset; }
    public void setValuationMethodOverrideReset(Boolean v) { this.valuationMethodOverrideReset = v; }
    public UUID getStockValuationAccountIdOverride() { return stockValuationAccountIdOverride; }
    public void setStockValuationAccountIdOverride(UUID v) { this.stockValuationAccountIdOverride = v; }
    public UUID getStockInputAccountIdOverride() { return stockInputAccountIdOverride; }
    public void setStockInputAccountIdOverride(UUID v) { this.stockInputAccountIdOverride = v; }
    public UUID getStockOutputAccountIdOverride() { return stockOutputAccountIdOverride; }
    public void setStockOutputAccountIdOverride(UUID v) { this.stockOutputAccountIdOverride = v; }
    public UUID getCogsAccountIdOverride() { return cogsAccountIdOverride; }
    public void setCogsAccountIdOverride(UUID v) { this.cogsAccountIdOverride = v; }
}
