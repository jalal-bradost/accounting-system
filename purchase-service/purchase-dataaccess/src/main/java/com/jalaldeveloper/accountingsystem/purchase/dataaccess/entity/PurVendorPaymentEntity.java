package com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorPaymentState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pur_vendor_payment", indexes = {
        @Index(name = "ix_pur_vp_company", columnList = "company_id,state"),
        @Index(name = "ix_pur_vp_bill", columnList = "vendor_bill_id")
})
public class PurVendorPaymentEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "vendor_partner_id", nullable = false)
    private UUID vendorPartnerId;

    @Column(name = "vendor_bill_id", nullable = false)
    private UUID vendorBillId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "bank_journal_id", nullable = false)
    private UUID bankJournalId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate_to_company", nullable = false, precision = 19, scale = 12)
    private BigDecimal exchangeRateToCompany = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorPaymentState state;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(length = 255)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PurVendorPaymentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getVendorPartnerId() { return vendorPartnerId; }
    public void setVendorPartnerId(UUID vendorPartnerId) { this.vendorPartnerId = vendorPartnerId; }
    public UUID getVendorBillId() { return vendorBillId; }
    public void setVendorBillId(UUID vendorBillId) { this.vendorBillId = vendorBillId; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
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
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
