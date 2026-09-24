package com.bradox.erp.accounting.service.domain.customerinvoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerPaymentResponse {
    private UUID id;
    private UUID companyId;
    private UUID customerPartnerId;
    private UUID customerInvoiceId;
    private LocalDateTime paymentDate;
    private UUID paymentJournalId;
    private BigDecimal amount;
    private String currencyCode;
    private BigDecimal exchangeRateToCompany;
    private UUID journalEntryId;
    private UUID reconciliationId;
    private UUID reversalJournalEntryId;
    private String reference;
    private String state;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(UUID reconciliationId) { this.reconciliationId = reconciliationId; }
    public UUID getReversalJournalEntryId() { return reversalJournalEntryId; }
    public void setReversalJournalEntryId(UUID reversalJournalEntryId) { this.reversalJournalEntryId = reversalJournalEntryId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
