package com.jalaldeveloper.accountingsystem.sales.service.domain.dto;

import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderDeliveryStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderInvoiceStatus;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SalesOrderSummaryResponse {

    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private String name;
    private SalesOrderState state;
    private SalesOrderDeliveryStatus deliveryStatus;
    private SalesOrderInvoiceStatus invoiceStatus;
    private String currencyCode;
    private LocalDate orderDate;
    private BigDecimal amountTotal;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SalesOrderState getState() { return state; }
    public void setState(SalesOrderState state) { this.state = state; }
    public SalesOrderDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(SalesOrderDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public SalesOrderInvoiceStatus getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(SalesOrderInvoiceStatus invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
