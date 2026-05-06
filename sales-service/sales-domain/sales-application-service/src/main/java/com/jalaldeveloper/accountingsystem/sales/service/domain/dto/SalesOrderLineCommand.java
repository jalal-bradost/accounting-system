package com.jalaldeveloper.accountingsystem.sales.service.domain.dto;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesOrderLineCommand {

    @NotNull
    private UUID productId;
    @NotBlank
    private String name;
    @NotNull
    private UUID uomId;
    @NotNull
    @Positive
    private BigDecimal qtyOrdered;
    /** When null, resolved from pricelist / product list price. */
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private List<UUID> taxIds = new ArrayList<>();
    private UUID revenueAccountId;
    private SalInvoicePolicy invoicePolicy;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds != null ? taxIds : new ArrayList<>(); }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public SalInvoicePolicy getInvoicePolicy() { return invoicePolicy; }
    public void setInvoicePolicy(SalInvoicePolicy invoicePolicy) { this.invoicePolicy = invoicePolicy; }
}
