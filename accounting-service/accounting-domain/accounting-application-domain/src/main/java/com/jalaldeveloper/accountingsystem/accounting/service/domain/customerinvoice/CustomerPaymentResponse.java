package com.jalaldeveloper.accountingsystem.accounting.service.domain.customerinvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CustomerPaymentResponse {
    private UUID id;
    private UUID companyId;
    private UUID customerInvoiceId;
    private LocalDate paymentDate;
    private UUID paymentJournalId;
    private BigDecimal amount;
    private String currencyCode;
    private BigDecimal exchangeRateToCompany;
    private UUID journalEntryId;
    private UUID reconciliationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
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
}
