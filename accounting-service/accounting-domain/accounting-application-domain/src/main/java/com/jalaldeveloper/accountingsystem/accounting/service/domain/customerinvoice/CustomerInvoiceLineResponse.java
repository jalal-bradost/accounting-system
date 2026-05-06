package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerInvoiceLineResponse {
    private UUID id;
    private int sequence;
    private String name;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private UUID revenueAccountId;
    private UUID salesOrderLineId;
    private List<CustomerInvoiceLineTaxResponse> taxSnapshots = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
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
    public List<CustomerInvoiceLineTaxResponse> getTaxSnapshots() { return taxSnapshots; }
    public void setTaxSnapshots(List<CustomerInvoiceLineTaxResponse> taxSnapshots) {
        this.taxSnapshots = taxSnapshots != null ? taxSnapshots : new ArrayList<>();
    }
}
