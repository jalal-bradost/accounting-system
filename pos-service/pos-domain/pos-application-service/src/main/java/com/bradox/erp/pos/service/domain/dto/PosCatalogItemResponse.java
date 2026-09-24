package com.bradox.erp.pos.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PosCatalogItemResponse {
    private UUID productId;
    private String sku;
    private String name;
    private String barcode;
    private UUID uomId;
    private UUID packagingId;
    private String packagingName;
    private BigDecimal qtyPerPackage;
    private BigDecimal listPrice;
    private BigDecimal purchasePrice;
    private boolean saleOk;
    private UUID categoryId;
    private String categoryName;
    private String imageUrl;
    private String productType;
    private BigDecimal qtyOnHand;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public UUID getPackagingId() { return packagingId; }
    public void setPackagingId(UUID packagingId) { this.packagingId = packagingId; }
    public String getPackagingName() { return packagingName; }
    public void setPackagingName(String packagingName) { this.packagingName = packagingName; }
    public BigDecimal getQtyPerPackage() { return qtyPerPackage; }
    public void setQtyPerPackage(BigDecimal qtyPerPackage) { this.qtyPerPackage = qtyPerPackage; }
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public boolean isSaleOk() { return saleOk; }
    public void setSaleOk(boolean saleOk) { this.saleOk = saleOk; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public BigDecimal getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(BigDecimal qtyOnHand) { this.qtyOnHand = qtyOnHand; }
}
