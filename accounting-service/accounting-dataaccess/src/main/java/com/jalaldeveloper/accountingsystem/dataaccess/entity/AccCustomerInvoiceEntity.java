package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceState;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "acc_customer_invoice", indexes = {
        @Index(name = "ix_acc_ci_company_state", columnList = "company_id,state"),
        @Index(name = "ix_acc_ci_partner", columnList = "customer_partner_id")
})
public class AccCustomerInvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "customer_partner_id", nullable = false)
    private UUID customerPartnerId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 255)
    private String reference;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerInvoiceState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false, length = 32)
    private CustomerInvoiceMoveType moveType = CustomerInvoiceMoveType.INVOICE;

    @Column(name = "reversed_invoice_id")
    private UUID reversedInvoiceId;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "exchange_rate_to_company", precision = 19, scale = 8)
    private BigDecimal exchangeRateToCompany;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<AccCustomerInvoiceLineEntity> lines = new ArrayList<>();

    public AccCustomerInvoiceEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public CustomerInvoiceState getState() { return state; }
    public void setState(CustomerInvoiceState state) { this.state = state; }
    public CustomerInvoiceMoveType getMoveType() { return moveType; }
    public void setMoveType(CustomerInvoiceMoveType moveType) {
        this.moveType = moveType != null ? moveType : CustomerInvoiceMoveType.INVOICE;
    }
    public UUID getReversedInvoiceId() { return reversedInvoiceId; }
    public void setReversedInvoiceId(UUID reversedInvoiceId) { this.reversedInvoiceId = reversedInvoiceId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public BigDecimal getExchangeRateToCompany() { return exchangeRateToCompany; }
    public void setExchangeRateToCompany(BigDecimal exchangeRateToCompany) { this.exchangeRateToCompany = exchangeRateToCompany; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<AccCustomerInvoiceLineEntity> getLines() { return lines; }
    public void setLines(List<AccCustomerInvoiceLineEntity> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
}
