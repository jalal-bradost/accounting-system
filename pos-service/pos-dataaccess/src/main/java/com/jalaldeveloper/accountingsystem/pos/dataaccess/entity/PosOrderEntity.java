package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pos_order", indexes = {
        @Index(name = "ix_pos_order_company_state", columnList = "company_id,state"),
        @Index(name = "ix_pos_order_session", columnList = "session_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_pos_order_company_name", columnNames = {"company_id", "name"}))
public class PosOrderEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Column(name = "customer_partner_id", nullable = false)
    private UUID customerPartnerId;
    @Column(nullable = false, length = 64)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PosOrderState state;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "amount_untaxed", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountUntaxed;
    @Column(name = "amount_tax", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTax;
    @Column(name = "amount_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTotal;
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;
    @Column(length = 4000)
    private String note;
    @Column(name = "sales_order_id")
    private UUID salesOrderId;
    @Column(name = "customer_invoice_id")
    private UUID customerInvoiceId;
    @Column(name = "receipt_id")
    private UUID receiptId;
    @Column(name = "finalized_at")
    private Instant finalizedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<PosOrderLineEntity> lines = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("paidAt ASC")
    private List<PosPaymentEntity> payments = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getCustomerPartnerId() { return customerPartnerId; }
    public void setCustomerPartnerId(UUID customerPartnerId) { this.customerPartnerId = customerPartnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PosOrderState getState() { return state; }
    public void setState(PosOrderState state) { this.state = state; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getAmountUntaxed() { return amountUntaxed; }
    public void setAmountUntaxed(BigDecimal amountUntaxed) { this.amountUntaxed = amountUntaxed; }
    public BigDecimal getAmountTax() { return amountTax; }
    public void setAmountTax(BigDecimal amountTax) { this.amountTax = amountTax; }
    public BigDecimal getAmountTotal() { return amountTotal; }
    public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public UUID getSalesOrderId() { return salesOrderId; }
    public void setSalesOrderId(UUID salesOrderId) { this.salesOrderId = salesOrderId; }
    public UUID getCustomerInvoiceId() { return customerInvoiceId; }
    public void setCustomerInvoiceId(UUID customerInvoiceId) { this.customerInvoiceId = customerInvoiceId; }
    public UUID getReceiptId() { return receiptId; }
    public void setReceiptId(UUID receiptId) { this.receiptId = receiptId; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getRowVersion() { return rowVersion; }
    public void setRowVersion(long rowVersion) { this.rowVersion = rowVersion; }
    public List<PosOrderLineEntity> getLines() { return lines; }
    public void setLines(List<PosOrderLineEntity> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public List<PosPaymentEntity> getPayments() { return payments; }
    public void setPayments(List<PosPaymentEntity> payments) { this.payments = payments != null ? payments : new ArrayList<>(); }
}
