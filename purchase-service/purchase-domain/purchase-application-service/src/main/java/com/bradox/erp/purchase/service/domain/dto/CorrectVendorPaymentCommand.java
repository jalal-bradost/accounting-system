package com.bradox.erp.purchase.service.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fields of a posted vendor payment that can be corrected; null keeps the current value. */
public class CorrectVendorPaymentCommand {

    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private UUID bankJournalId;
    private String reference;
    private String reason;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
