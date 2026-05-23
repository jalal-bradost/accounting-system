package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class UpdatePosOrderLineCommand {
    @Positive
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    @PositiveOrZero
    private BigDecimal discountPercent;
    private List<UUID> taxIds;

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds; }
}
