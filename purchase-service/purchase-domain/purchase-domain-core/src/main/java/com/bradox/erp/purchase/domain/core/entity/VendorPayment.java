package com.bradox.erp.purchase.domain.core.entity;

import com.bradox.erp.purchase.domain.core.VendorPaymentKind;
import com.bradox.erp.purchase.domain.core.VendorPaymentState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class VendorPayment {

    private UUID id;
    private UUID companyId;
    private UUID vendorPartnerId;
    private UUID vendorBillId;
    private LocalDateTime paymentDate;
    private UUID bankJournalId;
    private BigDecimal amount;
    private String currencyCode;
    private BigDecimal exchangeRateToCompany = BigDecimal.ONE;
    private VendorPaymentState state;
    private VendorPaymentKind paymentKind = VendorPaymentKind.PAYOUT;
    private UUID journalEntryId;
    private UUID reversalJournalEntryId;
    private String reference;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getBankJournalId() { return bankJournalId; }
    public void setBankJournalId(UUID bankJournalId) { this.bankJournalId = bankJournalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public VendorPaymentState getState() { return state; }
    public void setState(VendorPaymentState state) { this.state = state; }
    public VendorPaymentKind getPaymentKind() { return paymentKind; }
    public void setPaymentKind(VendorPaymentKind paymentKind) {
        this.paymentKind = paymentKind != null ? paymentKind : VendorPaymentKind.PAYOUT;
    }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getReversalJournalEntryId() { return reversalJournalEntryId; }
    public void setReversalJournalEntryId(UUID reversalJournalEntryId) { this.reversalJournalEntryId = reversalJournalEntryId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
