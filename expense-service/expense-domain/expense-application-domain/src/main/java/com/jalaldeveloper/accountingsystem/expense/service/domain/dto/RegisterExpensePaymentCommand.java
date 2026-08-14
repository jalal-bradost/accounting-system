package com.jalaldeveloper.accountingsystem.expense.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class RegisterExpensePaymentCommand {

    /** Cash or bank journal; liquidity account is resolved from the journal. */
    @NotNull
    private UUID bankJournalId;

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String currencyCode;
    private String reference;

    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
