package com.bradox.erp.dataaccess.entity;

import com.bradox.erp.domain.core.ValueObject.CustomerPaymentState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "acc_customer_payment", indexes = {
        @Index(name = "ix_acc_cp_company", columnList = "company_id"),
        @Index(name = "ix_acc_cp_invoice", columnList = "customer_invoice_id"),
        @Index(name = "ix_acc_cp_company_state", columnList = "company_id,state")
})
public class AccCustomerPaymentEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "customer_partner_id", nullable = false)
    private UUID customerPartnerId;

    @Column(name = "customer_invoice_id", nullable = false)
    private UUID customerInvoiceId;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_journal_id", nullable = false)
    private UUID paymentJournalId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate_to_company", nullable = false, precision = 19, scale = 12)
    private BigDecimal exchangeRateToCompany = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerPaymentState state = CustomerPaymentState.POSTED;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "reversal_journal_entry_id")
    private UUID reversalJournalEntryId;

    @Column(length = 255)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AccCustomerPaymentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public UUID getPaymentJournalId() { return paymentJournalId; }
    public void setPaymentJournalId(UUID paymentJournalId) { this.paymentJournalId = paymentJournalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public CustomerPaymentState getState() { return state; }
    public void setState(CustomerPaymentState state) { this.state = state; }
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
