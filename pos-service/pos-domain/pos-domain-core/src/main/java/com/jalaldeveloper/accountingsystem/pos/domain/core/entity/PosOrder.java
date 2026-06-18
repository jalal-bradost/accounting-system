package com.jalaldeveloper.accountingsystem.pos.domain.core.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosOrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PosOrder {
    private UUID id;
    private UUID companyId;
    private UUID sessionId;
    private UUID customerPartnerId;
    private String name;
    private PosOrderState state;
    private String currencyCode;
    private BigDecimal amountUntaxed;
    private BigDecimal amountTax;
    private BigDecimal amountTotal;
    private BigDecimal amountPaid;
    private String note;
    private UUID salesOrderId;
    private UUID customerInvoiceId;
    private UUID receiptId;
    private Instant finalizedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private long rowVersion;
    private List<PosOrderLine> lines = new ArrayList<>();
    private List<PosPayment> payments = new ArrayList<>();

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
    public List<PosOrderLine> getLines() { return lines; }
    public void setLines(List<PosOrderLine> lines) { this.lines = lines != null ? lines : new ArrayList<>(); }
    public List<PosPayment> getPayments() { return payments; }
    public void setPayments(List<PosPayment> payments) { this.payments = payments != null ? payments : new ArrayList<>(); }
}
