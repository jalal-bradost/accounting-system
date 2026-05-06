package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.BaseEntity;
import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ProductId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockMoveId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One product line on a {@link StockPicking}. Lifecycle is independent from the parent
 * picking to support partial validation and backorders.
 *
 * <p>{@code unitCost} is set at validation time (after the valuation strategy has run) and
 * is the cost actually applied to the move (used to compute the picked value).
 */
public class StockMove extends BaseEntity<StockMoveId> {

    private StockPickingId pickingId;
    private final ProductId productId;
    private final UomId uomId;
    private final StockLocationId sourceLocationId;
    private final StockLocationId destinationLocationId;

    private BigDecimal demandQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal pickedQuantity;
    private Money unitCost;
    private MoveState state;
    private final UUID purchaseOrderLineId;
    private final UUID salesOrderLineId;

    private StockMove(Builder b) {
        super.setId(b.id);
        this.pickingId = b.pickingId;
        this.productId = b.productId;
        this.uomId = b.uomId;
        this.sourceLocationId = b.sourceLocationId;
        this.destinationLocationId = b.destinationLocationId;
        this.demandQuantity = b.demandQuantity != null ? b.demandQuantity : BigDecimal.ZERO;
        this.reservedQuantity = b.reservedQuantity != null ? b.reservedQuantity : BigDecimal.ZERO;
        this.pickedQuantity = b.pickedQuantity != null ? b.pickedQuantity : BigDecimal.ZERO;
        this.unitCost = b.unitCost != null ? b.unitCost : Money.ZERO;
        this.state = b.state != null ? b.state : MoveState.DRAFT;
        this.purchaseOrderLineId = b.purchaseOrderLineId;
        this.salesOrderLineId = b.salesOrderLineId;
    }

    public void validateInvariants() {
        if (productId == null) throw new InventoryDomainException("move.productId required");
        if (uomId == null) throw new InventoryDomainException("move.uomId required");
        if (sourceLocationId == null) throw new InventoryDomainException("move.sourceLocationId required");
        if (destinationLocationId == null) throw new InventoryDomainException("move.destinationLocationId required");
        if (sourceLocationId.equals(destinationLocationId)) {
            throw new InventoryDomainException("move source and destination must differ");
        }
        if (demandQuantity == null || demandQuantity.signum() <= 0) {
            throw new InventoryDomainException("move.demandQuantity must be > 0");
        }
    }

    public void confirm() {
        ensureState("confirm", MoveState.DRAFT);
        this.state = MoveState.CONFIRMED;
    }

    public void markAssigned(BigDecimal reserved) {
        if (reserved == null) reserved = BigDecimal.ZERO;
        if (reserved.compareTo(demandQuantity) >= 0) {
            this.reservedQuantity = demandQuantity;
            this.state = MoveState.ASSIGNED;
        } else if (reserved.signum() > 0) {
            this.reservedQuantity = reserved;
            this.state = MoveState.PARTIALLY_ASSIGNED;
        } else {
            this.reservedQuantity = BigDecimal.ZERO;
            this.state = MoveState.CONFIRMED;
        }
    }

    public void markDone(BigDecimal picked, Money unitCost) {
        ensureState("markDone", MoveState.ASSIGNED, MoveState.PARTIALLY_ASSIGNED, MoveState.CONFIRMED);
        if (picked == null || picked.signum() <= 0) {
            throw new InventoryDomainException("picked qty must be > 0 to mark a move done");
        }
        if (picked.compareTo(demandQuantity) > 0) {
            throw new InventoryDomainException(
                    "picked qty " + picked + " exceeds demand " + demandQuantity);
        }
        this.pickedQuantity = picked;
        this.unitCost = unitCost != null ? unitCost : Money.ZERO;
        this.state = MoveState.DONE;
    }

    public void cancel() {
        if (state == MoveState.DONE) {
            throw new InventoryDomainException("cannot cancel a DONE move");
        }
        this.reservedQuantity = BigDecimal.ZERO;
        this.state = MoveState.CANCELLED;
    }

    public Money pickedValue() {
        return new Money(unitCost.getAmount().multiply(pickedQuantity));
    }

    public BigDecimal backorderQuantity() {
        return demandQuantity.subtract(pickedQuantity).max(BigDecimal.ZERO);
    }

    private void ensureState(String op, MoveState... allowed) {
        for (MoveState s : allowed) {
            if (state == s) return;
        }
        throw new InventoryDomainException(
                "Cannot " + op + " a move in state " + state + " (allowed: any of " + java.util.Arrays.toString(allowed) + ")");
    }

    public void attachToPicking(StockPickingId pickingId) {
        if (this.pickingId != null && !this.pickingId.equals(pickingId)) {
            throw new InventoryDomainException("move already attached to picking " + this.pickingId.getId());
        }
        this.pickingId = pickingId;
    }

    public StockPickingId getPickingId() { return pickingId; }
    public ProductId getProductId() { return productId; }
    public UomId getUomId() { return uomId; }
    public StockLocationId getSourceLocationId() { return sourceLocationId; }
    public StockLocationId getDestinationLocationId() { return destinationLocationId; }
    public BigDecimal getDemandQuantity() { return demandQuantity; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public BigDecimal getPickedQuantity() { return pickedQuantity; }
    public Money getUnitCost() { return unitCost; }
    public MoveState getState() { return state; }
    public UUID getPurchaseOrderLineId() { return purchaseOrderLineId; }
    public UUID getSalesOrderLineId() { return salesOrderLineId; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private StockMoveId id;
        private StockPickingId pickingId;
        private ProductId productId;
        private UomId uomId;
        private StockLocationId sourceLocationId;
        private StockLocationId destinationLocationId;
        private BigDecimal demandQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal pickedQuantity;
        private Money unitCost;
        private MoveState state;
        private UUID purchaseOrderLineId;
        private UUID salesOrderLineId;

        public Builder id(StockMoveId v) { this.id = v; return this; }
        public Builder pickingId(StockPickingId v) { this.pickingId = v; return this; }
        public Builder productId(ProductId v) { this.productId = v; return this; }
        public Builder uomId(UomId v) { this.uomId = v; return this; }
        public Builder sourceLocationId(StockLocationId v) { this.sourceLocationId = v; return this; }
        public Builder destinationLocationId(StockLocationId v) { this.destinationLocationId = v; return this; }
        public Builder demandQuantity(BigDecimal v) { this.demandQuantity = v; return this; }
        public Builder reservedQuantity(BigDecimal v) { this.reservedQuantity = v; return this; }
        public Builder pickedQuantity(BigDecimal v) { this.pickedQuantity = v; return this; }
        public Builder unitCost(Money v) { this.unitCost = v; return this; }
        public Builder state(MoveState v) { this.state = v; return this; }
        public Builder purchaseOrderLineId(UUID v) { this.purchaseOrderLineId = v; return this; }
        public Builder salesOrderLineId(UUID v) { this.salesOrderLineId = v; return this; }
        public StockMove build() { return new StockMove(this); }
    }
}
