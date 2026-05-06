package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerInvoiceResponse {
    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String reference;
    private String currencyCode;
    private CustomerInvoiceState state;
    private UUID journalEntryId;
    private UUID salesOrderId;
    private BigDecimal exchangeRateToCompany;
    private List<CustomerInvoiceLineResponse> lines = new ArrayList<>();

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
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public List<CustomerInvoiceLineResponse> getLines() { return lines; }
    public void setLines(List<CustomerInvoiceLineResponse> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
