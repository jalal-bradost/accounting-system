package com.bradox.erp.purchase.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** One requested allocation of a payment to a bill (payout) or credit note (refund). */
public class VendorPaymentAllocationLine {

    @NotNull
    private UUID billId;
    @NotNull
    @Positive
    private BigDecimal amount;
    /**
     * Part of the payment (payment currency) this allocation uses, when the payment and the document
     * are in different currencies. Optional: derived from the two exchange rates when omitted.
     */
    private BigDecimal paymentAmount;

    public VendorPaymentAllocationLine() {}

    public VendorPaymentAllocationLine(UUID billId, BigDecimal amount) {
        this.billId = billId;
        this.amount = amount;
    }

    public UUID getBillId() { return billId; }
    public void setBillId(UUID billId) { this.billId = billId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
}
