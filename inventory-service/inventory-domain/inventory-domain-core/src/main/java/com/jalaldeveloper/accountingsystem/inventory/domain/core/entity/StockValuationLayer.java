package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationLayerId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * An append-only ledger entry recording a change in inventory value (mirrors Odoo's
 * stock.valuation.layer / SVL). One layer per stock move that affects valuation.
 *
 * <p>For FIFO, positive layers (receipts) get partially consumed by negative layers
 * (deliveries) using {@link #remainingQuantity} / {@link #remainingValue} until exhausted.
 * For AVCO, layers are produced for traceability but the running average is the source of
 * truth for outgoing cost.
 */
public class StockValuationLayer extends AggregateRoot<ValuationLayerId> {

    private final CompanyId companyId;
    private final ProductId productId;
    private final StockMoveId stockMoveId;
    private final ValuationMethod method;
    private final Instant occurredAt;

    private final BigDecimal quantity;
    private final Money unitCost;
    private final Money value;

    private BigDecimal remainingQuantity;
    private Money remainingValue;

    private UUID journalEntryId;

    private StockValuationLayer(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.productId = b.productId;
        this.stockMoveId = b.stockMoveId;
        this.method = b.method;
        this.occurredAt = b.occurredAt != null ? b.occurredAt : Instant.now();
        this.quantity = b.quantity;
        this.unitCost = b.unitCost != null ? b.unitCost : Money.ZERO;
        this.value = b.value != null ? b.value : Money.ZERO;
        this.remainingQuantity = b.remainingQuantity != null ? b.remainingQuantity : b.quantity;
        this.remainingValue = b.remainingValue != null ? b.remainingValue : this.value;
        this.journalEntryId = b.journalEntryId;
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (productId == null) throw new InventoryDomainException("productId required");
        if (method == null) throw new InventoryDomainException("valuation method required");
        if (quantity == null) throw new InventoryDomainException("quantity required");
    }

    /**
     * Consume up to {@code requestedQty} from a positive (receipt) layer for FIFO. Returns the
     * actually-consumed quantity (may be less than requested if the layer is partially
     * exhausted). Decreases {@link #remainingQuantity} and {@link #remainingValue} pro-rata.
     */
    public ConsumptionResult consume(BigDecimal requestedQty) {
        if (requestedQty == null || requestedQty.signum() <= 0) {
            throw new InventoryDomainException("requested consumption qty must be > 0");
        }
        if (remainingQuantity == null || remainingQuantity.signum() <= 0) {
            return new ConsumptionResult(BigDecimal.ZERO, Money.ZERO);
        }
        BigDecimal taken = requestedQty.min(remainingQuantity);
        Money valueTaken = taken.compareTo(remainingQuantity) == 0
                ? remainingValue
                : new Money(remainingValue.getAmount()
                        .multiply(taken)
                        .divide(remainingQuantity, 4, RoundingMode.HALF_UP));
        this.remainingQuantity = remainingQuantity.subtract(taken);
        this.remainingValue = remainingValue.subtract(valueTaken);
        return new ConsumptionResult(taken, valueTaken);
    }

    public void linkJournalEntry(UUID journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public CompanyId getCompanyId() { return companyId; }
    public ProductId getProductId() { return productId; }
    public StockMoveId getStockMoveId() { return stockMoveId; }
    public ValuationMethod getMethod() { return method; }
    public Instant getOccurredAt() { return occurredAt; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getUnitCost() { return unitCost; }
    public Money getValue() { return value; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public Money getRemainingValue() { return remainingValue; }
    public UUID getJournalEntryId() { return journalEntryId; }

    public static Builder builder() { return new Builder(); }

    public record ConsumptionResult(BigDecimal quantity, Money value) {}

    public static final class Builder {
        private ValuationLayerId id;
        private CompanyId companyId;
        private ProductId productId;
        private StockMoveId stockMoveId;
        private ValuationMethod method;
        private Instant occurredAt;
        private BigDecimal quantity;
        private Money unitCost;
        private Money value;
        private BigDecimal remainingQuantity;
        private Money remainingValue;
        private UUID journalEntryId;

        public Builder id(ValuationLayerId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder productId(ProductId v) { this.productId = v; return this; }
        public Builder stockMoveId(StockMoveId v) { this.stockMoveId = v; return this; }
        public Builder method(ValuationMethod v) { this.method = v; return this; }
        public Builder occurredAt(Instant v) { this.occurredAt = v; return this; }
        public Builder quantity(BigDecimal v) { this.quantity = v; return this; }
        public Builder unitCost(Money v) { this.unitCost = v; return this; }
        public Builder value(Money v) { this.value = v; return this; }
        public Builder remainingQuantity(BigDecimal v) { this.remainingQuantity = v; return this; }
        public Builder remainingValue(Money v) { this.remainingValue = v; return this; }
        public Builder journalEntryId(UUID v) { this.journalEntryId = v; return this; }
        public StockValuationLayer build() { return new StockValuationLayer(this); }
    }
}
