package com.bradox.erp.dataaccess.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TradeReconciliationRebuildId implements Serializable {

    private UUID companyId;
    private String side;

    public TradeReconciliationRebuildId() {}

    public TradeReconciliationRebuildId(UUID companyId, String side) {
        this.companyId = companyId;
        this.side = side;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeReconciliationRebuildId that)) return false;
        return Objects.equals(companyId, that.companyId) && Objects.equals(side, that.side);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, side);
    }
}
