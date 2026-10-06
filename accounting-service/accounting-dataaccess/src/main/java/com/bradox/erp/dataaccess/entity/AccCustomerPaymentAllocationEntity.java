package com.bradox.erp.dataaccess.entity;

import com.bradox.erp.domain.core.ValueObject.CustomerPaymentAllocationState;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "acc_customer_payment_allocation", indexes = {
        @Index(name = "ix_acc_cpa_payment", columnList = "payment_id"),
        @Index(name = "ix_acc_cpa_invoice_state", columnList = "customer_invoice_id,state"),
        @Index(name = "ix_acc_cpa_company", columnList = "company_id")
})
public class AccCustomerPaymentAllocationEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "customer_invoice_id", nullable = false)
    private UUID customerInvoiceId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Column(name = "amount_company", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountCompany;

    @Column(name = "payment_amount_company", nullable = false, precision = 19, scale = 4)
    private BigDecimal paymentAmountCompany;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "fx_journal_entry_id")
    private UUID fxJournalEntryId;

    @Column(name = "fx_reversal_journal_entry_id")
    private UUID fxReversalJournalEntryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerPaymentAllocationState state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
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
    public CustomerPaymentAllocationState getState() { return state; }
    public void setState(CustomerPaymentAllocationState state) { this.state = state; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
