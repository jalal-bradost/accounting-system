package com.bradox.erp.accounting.service.domain.customerinvoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** One requested allocation of a payment to an invoice (receipt) or credit note (refund). */
public class CustomerPaymentAllocationLine {

    @NotNull
    private UUID invoiceId;
    @NotNull
    @Positive
    private BigDecimal amount;
    /**
     * Part of the payment (payment currency) this allocation uses, when the payment and the document
     * are in different currencies. Optional: derived from the two exchange rates when omitted.
     */
    private BigDecimal paymentAmount;

    public CustomerPaymentAllocationLine() {}

    public CustomerPaymentAllocationLine(UUID invoiceId, BigDecimal amount) {
        this.invoiceId = invoiceId;
        this.amount = amount;
    }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
}
