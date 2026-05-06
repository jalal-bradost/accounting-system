package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockQuantId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * On-hand and reserved quantity for a {@code (companyId, productId, locationId)} tuple.
 * Mirrors Odoo's stock.quant. Mutated by stock moves; concurrent updates are protected by an
 * optimistic lock {@link #version}.
 *
 * <p>Reservation logic: {@code availableQty = quantity - reservedQuantity}. Outgoing moves
 * reserve before validation; on validation the quantity is decremented and the reservation
 * released.
 */
public class StockQuant extends AggregateRoot<StockQuantId> {

    private final CompanyId companyId;
    private final ProductId productId;
    private final StockLocationId locationId;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private Instant lastChangedAt;
    private long version;

    private StockQuant(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.productId = b.productId;
        this.locationId = b.locationId;
        this.quantity = b.quantity != null ? b.quantity : BigDecimal.ZERO;
        this.reservedQuantity = b.reservedQuantity != null ? b.reservedQuantity : BigDecimal.ZERO;
        this.lastChangedAt = b.lastChangedAt;
        this.version = b.version;
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (productId == null) throw new InventoryDomainException("productId required");
        if (locationId == null) throw new InventoryDomainException("locationId required");
    }

    public BigDecimal getAvailable() {
        return quantity.subtract(reservedQuantity);
    }

    /**
     * Apply a delta to on-hand quantity.
     * @param delta positive for incoming, negative for outgoing.
     * @param allowNegative whether the operation may take {@code quantity} below zero.
     */
    public void applyDelta(BigDecimal delta, boolean allowNegative) {
        if (delta == null) return;
        BigDecimal next = quantity.add(delta);
        if (!allowNegative && next.signum() < 0) {
            throw new InventoryDomainException(
                    "Negative stock not allowed at location " + locationId.getId()
                            + " for product " + productId.getId()
                            + " (current=" + quantity + ", delta=" + delta + ")");
        }
        if (delta.signum() < 0 && reservedQuantity.compareTo(quantity.add(delta)) > 0) {
            // The decrement crosses the reservation threshold; release reservations down to next qty.
            this.reservedQuantity = next.max(BigDecimal.ZERO);
        }
        this.quantity = next;
        this.lastChangedAt = Instant.now();
    }

    /** Reserve {@code qty} for an outgoing move. Throws if insufficient available. */
    public void reserve(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new InventoryDomainException("reserve quantity must be > 0");
        }
        if (qty.compareTo(getAvailable()) > 0) {
            throw new InventoryDomainException(
                    "Insufficient available qty to reserve at location " + locationId.getId()
                            + " (available=" + getAvailable() + ", requested=" + qty + ")");
        }
        this.reservedQuantity = this.reservedQuantity.add(qty);
        this.lastChangedAt = Instant.now();
    }

    /** Release a previously reserved quantity (e.g. on cancel or partial validate). */
    public void release(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) return;
        BigDecimal next = this.reservedQuantity.subtract(qty);
        this.reservedQuantity = next.signum() < 0 ? BigDecimal.ZERO : next;
        this.lastChangedAt = Instant.now();
    }

    public CompanyId getCompanyId() { return companyId; }
    public ProductId getProductId() { return productId; }
    public StockLocationId getLocationId() { return locationId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public Instant getLastChangedAt() { return lastChangedAt; }
    public long getVersion() { return version; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private StockQuantId id;
        private CompanyId companyId;
        private ProductId productId;
        private StockLocationId locationId;
        private BigDecimal quantity;
        private BigDecimal reservedQuantity;
        private Instant lastChangedAt;
        private long version;

        public Builder id(StockQuantId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder productId(ProductId v) { this.productId = v; return this; }
        public Builder locationId(StockLocationId v) { this.locationId = v; return this; }
        public Builder quantity(BigDecimal v) { this.quantity = v; return this; }
        public Builder reservedQuantity(BigDecimal v) { this.reservedQuantity = v; return this; }
        public Builder lastChangedAt(Instant v) { this.lastChangedAt = v; return this; }
        public Builder version(long v) { this.version = v; return this; }
        public StockQuant build() { return new StockQuant(this); }
    }
}
