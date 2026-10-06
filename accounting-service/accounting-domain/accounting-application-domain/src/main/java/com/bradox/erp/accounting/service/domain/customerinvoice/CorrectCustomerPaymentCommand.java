package com.bradox.erp.accounting.service.domain.customerinvoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fields of a posted payment that can be corrected; null keeps the current value. */
public class CorrectCustomerPaymentCommand {

    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private UUID paymentJournalId;
    private String reference;
    private String reason;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
