package com.jalaldeveloper.accountingsystem.purchase.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class RegisterVendorPaymentCommand {

    private UUID companyId;
    @NotNull private UUID vendorBillId;
    /** Cash or bank journal; liquidity account is resolved from the journal code (same as account code in default chart). */
    @NotNull private UUID bankJournalId;
    @NotNull private LocalDate paymentDate;
    @NotNull @Positive private BigDecimal amount;
    @NotNull private String currencyCode;
    private BigDecimal exchangeRateToCompany;
    private String reference;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
