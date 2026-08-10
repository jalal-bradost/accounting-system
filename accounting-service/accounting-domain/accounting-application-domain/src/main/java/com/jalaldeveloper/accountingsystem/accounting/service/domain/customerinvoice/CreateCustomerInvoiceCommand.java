package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateCustomerInvoiceCommand {

    private UUID companyId;
    @NotNull
    private UUID customerPartnerId;
    @NotNull
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    @NotNull
    private String currencyCode;
    private String reference;
    private UUID salesOrderId;
    private BigDecimal exchangeRateToCompany;
    /** INVOICE (default) or CREDIT_NOTE */
    private String moveType;
    private UUID reversedInvoiceId;
    @NotEmpty
    @Valid
    private List<CustomerInvoiceLineCommand> lines;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public String getMoveType() { return moveType; }
    public void setMoveType(String moveType) { this.moveType = moveType; }
    public UUID getReversedInvoiceId() { return reversedInvoiceId; }
    public void setReversedInvoiceId(UUID reversedInvoiceId) { this.reversedInvoiceId = reversedInvoiceId; }
    public List<CustomerInvoiceLineCommand> getLines() { return lines; }
    public void setLines(List<CustomerInvoiceLineCommand> lines) { this.lines = lines; }
}
