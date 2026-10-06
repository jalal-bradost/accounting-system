package com.bradox.erp.accounting.service.domain.customerinvoice;

import java.time.LocalDateTime;
import java.util.UUID;

/** Pays back, in cash or bank, the credit a customer has on a paid invoice that was credited. */
public class RefundCustomerCreditCommand {

    private UUID paymentJournalId;
    private LocalDateTime paymentDate;
    private String reference;

    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
