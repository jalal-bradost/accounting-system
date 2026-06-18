package com.jalaldeveloper.accountingsystem.pos.domain.core.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PosPayment {
    private UUID id;
    private PosOrder order;
    private PosPaymentMethod method;
    private UUID journalId;
    private BigDecimal amount;
    private String reference;
    private Instant paidAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PosOrder getOrder() { return order; }
    public void setOrder(PosOrder order) { this.order = order; }
    public PosPaymentMethod getMethod() { return method; }
    public void setMethod(PosPaymentMethod method) { this.method = method; }
    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
