package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.pos.domain.core.PosPaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_payment")
public class PosPaymentEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PosOrderEntity order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PosPaymentMethod method;
    @Column(name = "journal_id", nullable = false)
    private UUID journalId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(length = 255)
    private String reference;
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PosOrderEntity getOrder() { return order; }
    public void setOrder(PosOrderEntity order) { this.order = order; }
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
