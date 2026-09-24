package com.bradox.delin.accounting.service.domain.customerinvoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RegisterCustomerPaymentCommand {

    private UUID companyId;
    @NotNull
    private UUID customerInvoiceId;
    /** Cash (430001) or Bank (430002) journal from the chart — liquidity account matches journal code. */
    @NotNull
    private UUID paymentJournalId;
    @NotNull
    private LocalDateTime paymentDate;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotNull
    private String currencyCode;
    /** Document × rate = company currency. Defaults from company currency master on payment date. */
    private BigDecimal exchangeRateToCompany;
    private String reference;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
