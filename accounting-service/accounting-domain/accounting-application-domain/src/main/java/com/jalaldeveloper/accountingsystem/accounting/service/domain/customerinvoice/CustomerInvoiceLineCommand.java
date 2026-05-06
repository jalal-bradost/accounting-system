package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerInvoiceLineCommand {

    @NotBlank
    private String name;
    @NotNull
    @Positive
    private BigDecimal qty;
    @NotNull
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    /** When null, default sales revenue account (chart) is used. */
    private UUID revenueAccountId;
    private UUID salesOrderLineId;
    @Valid
    private List<CustomerInvoiceLineTaxCommand> taxSnapshots = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public UUID getSalesOrderLineId() { return salesOrderLineId; }
    public void setSalesOrderLineId(UUID salesOrderLineId) { this.salesOrderLineId = salesOrderLineId; }
    public List<CustomerInvoiceLineTaxCommand> getTaxSnapshots() { return taxSnapshots; }
    public void setTaxSnapshots(List<CustomerInvoiceLineTaxCommand> taxSnapshots) {
        this.taxSnapshots = taxSnapshots != null ? taxSnapshots : new ArrayList<>();
    }
}
