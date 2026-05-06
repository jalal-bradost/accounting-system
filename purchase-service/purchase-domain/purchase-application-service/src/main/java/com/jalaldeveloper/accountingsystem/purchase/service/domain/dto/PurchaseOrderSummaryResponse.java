package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Lightweight row for purchase order list & search. */
public class PurchaseOrderSummaryResponse {

    private UUID id;
    private UUID companyId;
    private UUID vendorPartnerId;
    private String name;
    private PurchaseOrderState state;
    private String currencyCode;
    private LocalDate orderDate;
    private BigDecimal amountTotal;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PurchaseOrderState getState() { return state; }
    public void setState(PurchaseOrderState state) { this.state = state; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
