package com.bradox.delin.inventory.service.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductPackagingCommand {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal qty;

    /** When null, server defaults to base product cost × qty. */
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal purchasePrice;

    /** When null, server defaults to base product list price × qty. */
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal listPrice;

    private String barcode;
    private String sku;
    private Boolean active;

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
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
