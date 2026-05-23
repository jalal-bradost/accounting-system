package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PosOrderLineCommand {
    @NotNull
    private UUID productId;
    private String name;
    private UUID uomId;
    @NotNull
    @Positive
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    @PositiveOrZero
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private List<UUID> taxIds = new ArrayList<>();
    private UUID revenueAccountId;

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
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds != null ? taxIds : new ArrayList<>(); }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
}
