package com.jalaldeveloper.accountingsystem.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerInvoice {

    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String reference;
    private String currencyCode;
    private CustomerInvoiceState state;
    private CustomerInvoiceMoveType moveType = CustomerInvoiceMoveType.INVOICE;
    private UUID reversedInvoiceId;
    private UUID journalEntryId;
    private UUID salesOrderId;
    private BigDecimal exchangeRateToCompany;
    private long rowVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CustomerInvoiceLine> lines = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public CustomerInvoiceState getState() { return state; }
    public void setState(CustomerInvoiceState state) { this.state = state; }
    public CustomerInvoiceMoveType getMoveType() { return moveType; }
    public void setMoveType(CustomerInvoiceMoveType moveType) {
        this.moveType = moveType != null ? moveType : CustomerInvoiceMoveType.INVOICE;
    }
    public UUID getReversedInvoiceId() { return reversedInvoiceId; }
    public void setReversedInvoiceId(UUID reversedInvoiceId) { this.reversedInvoiceId = reversedInvoiceId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<CustomerInvoiceLine> getLines() { return lines; }
    public void setLines(List<CustomerInvoiceLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
