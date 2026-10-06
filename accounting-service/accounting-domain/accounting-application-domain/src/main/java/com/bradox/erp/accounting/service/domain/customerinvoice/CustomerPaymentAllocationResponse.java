package com.bradox.erp.accounting.service.domain.customerinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerPaymentAllocationResponse {
    private UUID id;
    private UUID paymentId;
    private String paymentReference;
    private LocalDateTime paymentDate;
    private UUID customerInvoiceId;
    private String invoiceReference;
    private String invoiceMoveType;
    private boolean invoiceOpeningBalance;
    private BigDecimal amount;
    private BigDecimal paymentAmount;
    private String currencyCode;
    private BigDecimal amountCompany;
    private BigDecimal paymentAmountCompany;
    private LocalDate allocationDate;
    private UUID fxJournalEntryId;
    private UUID fxReversalJournalEntryId;
    /** ACTIVE | REVERSED */
    private String state;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public String getInvoiceReference() { return invoiceReference; }
    public void setInvoiceReference(String invoiceReference) { this.invoiceReference = invoiceReference; }
    public String getInvoiceMoveType() { return invoiceMoveType; }
    public void setInvoiceMoveType(String invoiceMoveType) { this.invoiceMoveType = invoiceMoveType; }
    public boolean isInvoiceOpeningBalance() { return invoiceOpeningBalance; }
    public void setInvoiceOpeningBalance(boolean invoiceOpeningBalance) { this.invoiceOpeningBalance = invoiceOpeningBalance; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getAmountCompany() { return amountCompany; }
    public void setAmountCompany(BigDecimal amountCompany) { this.amountCompany = amountCompany; }
    public BigDecimal getPaymentAmountCompany() { return paymentAmountCompany; }
    public void setPaymentAmountCompany(BigDecimal paymentAmountCompany) { this.paymentAmountCompany = paymentAmountCompany; }
    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }
    public UUID getFxJournalEntryId() { return fxJournalEntryId; }
    public void setFxJournalEntryId(UUID fxJournalEntryId) { this.fxJournalEntryId = fxJournalEntryId; }
    public UUID getFxReversalJournalEntryId() { return fxReversalJournalEntryId; }
    public void setFxReversalJournalEntryId(UUID fxReversalJournalEntryId) { this.fxReversalJournalEntryId = fxReversalJournalEntryId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
