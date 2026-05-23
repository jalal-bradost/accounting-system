package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PosCatalogItemResponse {
    private UUID productId;
    private String sku;
    private String name;
    private String barcode;
    private UUID uomId;
    private BigDecimal listPrice;
    private boolean saleOk;

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
    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }
    public boolean isSaleOk() { return saleOk; }
    public void setSaleOk(boolean saleOk) { this.saleOk = saleOk; }
}
