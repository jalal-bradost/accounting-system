package com.jalaldeveloper.accountingsystem.inventory.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only ledger entry tracking inventory value changes. For FIFO, positive layers
 * track {@code remaining_quantity} / {@code remaining_value} as they get consumed by
 * outgoing moves.
 */
@Entity
@Table(name = "inv_stock_valuation_layer", indexes = {
        @Index(name = "ix_inv_svl_company_product", columnList = "company_id,product_id"),
        @Index(name = "ix_inv_svl_fifo", columnList = "company_id,product_id,occurred_at"),
        @Index(name = "ix_inv_svl_journal", columnList = "journal_entry_id")
})
public class StockValuationLayerEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "stock_move_id")
    private UUID stockMoveId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValuationMethod method;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Column(name = "remaining_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingQuantity;

    @Column(name = "remaining_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingValue;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    public StockValuationLayerEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
