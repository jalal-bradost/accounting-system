package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PosOrderLineResponse {
    private UUID id;
    private UUID productId;
    private String name;
    private UUID uomId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private List<UUID> taxIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds != null ? taxIds : new ArrayList<>(); }
}
