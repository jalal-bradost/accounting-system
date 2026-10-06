package com.bradox.erp.purchase.domain.core.entity;

import com.bradox.erp.purchase.domain.core.VendorPaymentAllocationState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Links part of a vendor payment to one bill (payouts) or credit note (refunds).
 * {@code amount} is in the shared payment/document currency; {@code amountCompany} is the
 * payable value settled on the document side and {@code paymentAmountCompany} on the payment
 * side. Their difference is booked by the FX entry.
 */
public class VendorPaymentAllocation {

    private UUID id;
    private UUID companyId;
    private UUID paymentId;
    private UUID vendorBillId;
    private BigDecimal amount;
    /** The part of the payment (payment currency) used by this allocation; equals amount when the currencies match. */
    private BigDecimal paymentAmount;
    private BigDecimal amountCompany;
    private BigDecimal paymentAmountCompany;
    private LocalDate allocationDate;
    private UUID fxJournalEntryId;
    private UUID fxReversalJournalEntryId;
    private VendorPaymentAllocationState state = VendorPaymentAllocationState.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isActive() { return state == VendorPaymentAllocationState.ACTIVE; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaymentAmount() { return paymentAmount != null ? paymentAmount : amount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getAmountCompany() { return amountCompany; }
    public void setAmountCompany(BigDecimal amountCompany) { this.amountCompany = amountCompany; }
    public BigDecimal getPaymentAmountCompany() { return paymentAmountCompany; }
    public void setPaymentAmountCompany(BigDecimal paymentAmountCompany) { this.paymentAmountCompany = paymentAmountCompany; }
    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }
    public UUID getFxJournalEntryId() { return fxJournalEntryId; }
    public void setFxJournalEntryId(UUID fxJournalEntryId) { this.fxJournalEntryId = fxJournalEntryId; }
    public UUID getFxReversalJournalEntryId() { return fxReversalJournalEntryId; }
    public void setFxReversalJournalEntryId(UUID fxReversalJournalEntryId) { this.fxReversalJournalEntryId = fxReversalJournalEntryId; }
    public VendorPaymentAllocationState getState() { return state; }
    public void setState(VendorPaymentAllocationState state) { this.state = state; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
