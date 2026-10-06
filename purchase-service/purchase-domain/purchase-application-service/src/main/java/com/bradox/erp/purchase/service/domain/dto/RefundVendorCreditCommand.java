package com.bradox.erp.purchase.service.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Collects, in cash or bank, the credit we hold with a vendor on a paid bill that was credited. */
public class RefundVendorCreditCommand {

    private UUID bankJournalId;
    private LocalDateTime paymentDate;
    private String reference;

    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
