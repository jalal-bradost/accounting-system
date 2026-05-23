package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_receipt", indexes = {
        @Index(name = "ix_pos_receipt_company", columnList = "company_id"),
        @Index(name = "ix_pos_receipt_order", columnList = "order_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_pos_receipt_company_number", columnNames = {"company_id", "receipt_number"}))
public class PosReceiptEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "receipt_number", nullable = false, length = 64)
    private String receiptNumber;
    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
