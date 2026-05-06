package com.jalaldeveloper.accountingsystem.sales.service.domain.dto;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalInvoicePolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SalesOrderLineResponse {

    private UUID id;
    private int sequence;
    private UUID productId;
    private String name;
    private UUID uomId;
    private BigDecimal qtyOrdered;
    private BigDecimal qtyDelivered;
    private BigDecimal qtyInvoiced;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private SalInvoicePolicy invoicePolicy;
    private UUID revenueAccountId;
    private List<UUID> taxIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }
    public BigDecimal getQtyDelivered() { return qtyDelivered; }
    public void setQtyDelivered(BigDecimal qtyDelivered) { this.qtyDelivered = qtyDelivered; }
    public BigDecimal getQtyInvoiced() { return qtyInvoiced; }
    public void setQtyInvoiced(BigDecimal qtyInvoiced) { this.qtyInvoiced = qtyInvoiced; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public SalInvoicePolicy getInvoicePolicy() { return invoicePolicy; }
    public void setInvoicePolicy(SalInvoicePolicy invoicePolicy) { this.invoicePolicy = invoicePolicy; }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds != null ? taxIds : new ArrayList<>(); }
}
