package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ValuationLayerResponse {
    private UUID id;
    private UUID companyId;
    private UUID productId;
    private UUID stockMoveId;
    private ValuationMethod method;
    private Instant occurredAt;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal value;
    private BigDecimal remainingQuantity;
    private BigDecimal remainingValue;
    private UUID journalEntryId;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID v) { this.companyId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public UUID getStockMoveId() { return stockMoveId; }
    public void setStockMoveId(UUID v) { this.stockMoveId = v; }
    public ValuationMethod getMethod() { return method; }
    public void setMethod(ValuationMethod v) { this.method = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { this.occurredAt = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal v) { this.unitCost = v; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal v) { this.value = v; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal v) { this.remainingQuantity = v; }
    public BigDecimal getRemainingValue() { return remainingValue; }
    public void setRemainingValue(BigDecimal v) { this.remainingValue = v; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID v) { this.journalEntryId = v; }
}
