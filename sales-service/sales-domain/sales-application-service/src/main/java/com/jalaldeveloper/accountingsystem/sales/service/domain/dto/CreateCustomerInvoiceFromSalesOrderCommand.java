package com.jalaldeveloper.accountingsystem.sales.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateCustomerInvoiceFromSalesOrderCommand {

    private java.util.UUID companyId;
    @NotNull
    private java.util.UUID salesOrderId;
    private java.util.UUID sourceInvoiceId;
    @NotNull
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String reference;

    public java.util.UUID getCompanyId() { return companyId; }
    public void setCompanyId(java.util.UUID companyId) { this.companyId = companyId; }
    public java.util.UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(java.util.UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public java.util.UUID getSourceInvoiceId() { return sourceInvoiceId; }
    public void setSourceInvoiceId(java.util.UUID sourceInvoiceId) { this.sourceInvoiceId = sourceInvoiceId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
