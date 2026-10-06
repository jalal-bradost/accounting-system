package com.bradox.erp.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "acc_trade_reconciliation_rebuild")
@IdClass(TradeReconciliationRebuildId.class)
public class AccTradeReconciliationRebuildEntity {

    @Id
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Id
    @Column(nullable = false, length = 16)
    private String side;

    @Column(name = "rebuilt_at", nullable = false)
    private Instant rebuiltAt;

    public AccTradeReconciliationRebuildEntity() {}

    public AccTradeReconciliationRebuildEntity(UUID companyId, String side, Instant rebuiltAt) {
        this.companyId = companyId;
        this.side = side;
        this.rebuiltAt = rebuiltAt;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public Instant getRebuiltAt() { return rebuiltAt; }
    public void setRebuiltAt(Instant rebuiltAt) { this.rebuiltAt = rebuiltAt; }
}
