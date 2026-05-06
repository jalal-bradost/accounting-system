package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.AggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.MoveState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingState;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.PickingType;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockLocationId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.StockPickingId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.WarehouseId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for one logical stock operation: a receipt, a delivery, an internal
 * transfer, or a return. Owns its {@link StockMove} lines and the picking-level state
 * machine (DRAFT -&gt; CONFIRMED -&gt; ASSIGNED -&gt; DONE | CANCELLED).
 *
 * <p>Both the picking and its moves carry their own state. The picking state is derived from
 * the aggregate of move states by callers (application service) but the explicit transitions
 * here keep invariants (e.g. you cannot cancel a DONE picking).
 */
public class StockPicking extends AggregateRoot<StockPickingId> {

    private final CompanyId companyId;
    private final WarehouseId warehouseId;
    private final PickingType pickingType;
    private String reference;
    private final StockLocationId sourceLocationId;
    private final StockLocationId destinationLocationId;
    private UUID partnerId;
    private String origin;
    private Instant scheduledAt;
    private Instant validatedAt;
    private String validatedBy;
    private PickingState state;
    private final List<StockMove> moves = new ArrayList<>();
    private StockPickingId backorderOf;
    private UUID purchaseOrderId;
    private UUID salesOrderId;

    private StockPicking(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.warehouseId = b.warehouseId;
        this.pickingType = b.pickingType;
        this.reference = b.reference;
        this.sourceLocationId = b.sourceLocationId;
        this.destinationLocationId = b.destinationLocationId;
        this.partnerId = b.partnerId;
        this.origin = b.origin;
        this.scheduledAt = b.scheduledAt;
        this.validatedAt = b.validatedAt;
        this.validatedBy = b.validatedBy;
        this.state = b.state != null ? b.state : PickingState.DRAFT;
        if (b.moves != null) {
            for (StockMove move : b.moves) {
                this.moves.add(move);
                if (this.getId() != null) move.attachToPicking(this.getId());
            }
        }
        this.backorderOf = b.backorderOf;
        this.purchaseOrderId = b.purchaseOrderId;
        this.salesOrderId = b.salesOrderId;
    }

    public void validateInvariants() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (pickingType == null) throw new InventoryDomainException("pickingType required");
        if (sourceLocationId == null) throw new InventoryDomainException("sourceLocationId required");
        if (destinationLocationId == null) throw new InventoryDomainException("destinationLocationId required");
        if (sourceLocationId.equals(destinationLocationId)) {
            throw new InventoryDomainException("picking source and destination must differ");
        }
        if (moves.isEmpty()) throw new InventoryDomainException("picking must have at least one move");
        for (StockMove m : moves) m.validateInvariants();
    }

    public StockMove addMove(StockMove move) {
        if (state != PickingState.DRAFT) {
            throw new InventoryDomainException("Can only add moves to a DRAFT picking; current=" + state);
        }
        if (move == null) throw new InventoryDomainException("move required");
        if (this.getId() != null) move.attachToPicking(this.getId());
        moves.add(move);
        return move;
    }

    public void confirm() {
        ensureState("confirm", PickingState.DRAFT);
        validateInvariants();
        for (StockMove m : moves) m.confirm();
        this.state = PickingState.CONFIRMED;
    }

    /** Recompute the picking state from the aggregated move states (called after assign or partial validate). */
    public void recomputeState() {
        if (state == PickingState.DONE || state == PickingState.CANCELLED) return;
        boolean allDone = !moves.isEmpty();
        boolean allAssigned = !moves.isEmpty();
        for (StockMove m : moves) {
            if (m.getState() != MoveState.DONE && m.getState() != MoveState.CANCELLED) allDone = false;
            if (m.getState() != MoveState.ASSIGNED && m.getState() != MoveState.DONE && m.getState() != MoveState.CANCELLED) {
                allAssigned = false;
            }
        }
        if (allDone) {
            this.state = PickingState.DONE;
            this.validatedAt = Instant.now();
        } else if (allAssigned) {
            this.state = PickingState.ASSIGNED;
        } else {
            this.state = PickingState.CONFIRMED;
        }
    }

    public void markValidated(String userId) {
        ensureState("validate", PickingState.CONFIRMED, PickingState.ASSIGNED);
        for (StockMove m : moves) {
            if (m.getState() != MoveState.DONE && m.getState() != MoveState.CANCELLED) {
                throw new InventoryDomainException(
                        "Cannot validate picking: move " + m.getId() + " is in state " + m.getState());
            }
        }
        this.state = PickingState.DONE;
        this.validatedAt = Instant.now();
        this.validatedBy = userId;
    }

    public void cancel() {
        if (state == PickingState.DONE) {
            throw new InventoryDomainException("cannot cancel a DONE picking");
        }
        for (StockMove m : moves) {
            if (m.getState() != MoveState.DONE) m.cancel();
        }
        this.state = PickingState.CANCELLED;
    }

    public void changeReference(String reference) { this.reference = reference; }
    public void changePartner(UUID partnerId) { this.partnerId = partnerId; }
    public void changeOrigin(String origin) { this.origin = origin; }
    public void changeScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public void linkBackorderOf(StockPickingId pickingId) { this.backorderOf = pickingId; }

    private void ensureState(String op, PickingState... allowed) {
        for (PickingState s : allowed) {
            if (state == s) return;
        }
        throw new InventoryDomainException(
                "Cannot " + op + " a picking in state " + state + " (allowed: any of " + java.util.Arrays.toString(allowed) + ")");
    }

    public CompanyId getCompanyId() { return companyId; }
    public WarehouseId getWarehouseId() { return warehouseId; }
    public PickingType getPickingType() { return pickingType; }
    public String getReference() { return reference; }
    public StockLocationId getSourceLocationId() { return sourceLocationId; }
    public StockLocationId getDestinationLocationId() { return destinationLocationId; }
    public UUID getPartnerId() { return partnerId; }
    public String getOrigin() { return origin; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getValidatedAt() { return validatedAt; }
    public String getValidatedBy() { return validatedBy; }
    public PickingState getState() { return state; }
    public List<StockMove> getMoves() { return List.copyOf(moves); }
    public StockPickingId getBackorderOf() { return backorderOf; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getSalesOrderId() { return salesOrderId; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private StockPickingId id;
        private CompanyId companyId;
        private WarehouseId warehouseId;
        private PickingType pickingType;
        private String reference;
        private StockLocationId sourceLocationId;
        private StockLocationId destinationLocationId;
        private UUID partnerId;
        private String origin;
        private Instant scheduledAt;
        private Instant validatedAt;
        private String validatedBy;
        private PickingState state;
        private List<StockMove> moves;
        private StockPickingId backorderOf;
        private UUID purchaseOrderId;
        private UUID salesOrderId;

        public Builder id(StockPickingId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder warehouseId(WarehouseId v) { this.warehouseId = v; return this; }
        public Builder pickingType(PickingType v) { this.pickingType = v; return this; }
        public Builder reference(String v) { this.reference = v; return this; }
        public Builder sourceLocationId(StockLocationId v) { this.sourceLocationId = v; return this; }
        public Builder destinationLocationId(StockLocationId v) { this.destinationLocationId = v; return this; }
        public Builder partnerId(UUID v) { this.partnerId = v; return this; }
        public Builder origin(String v) { this.origin = v; return this; }
        public Builder scheduledAt(Instant v) { this.scheduledAt = v; return this; }
        public Builder validatedAt(Instant v) { this.validatedAt = v; return this; }
        public Builder validatedBy(String v) { this.validatedBy = v; return this; }
        public Builder state(PickingState v) { this.state = v; return this; }
        public Builder moves(List<StockMove> v) { this.moves = v; return this; }
        public Builder backorderOf(StockPickingId v) { this.backorderOf = v; return this; }
        public Builder purchaseOrderId(UUID v) { this.purchaseOrderId = v; return this; }
        public Builder salesOrderId(UUID v) { this.salesOrderId = v; return this; }
        public StockPicking build() { return new StockPicking(this); }
    }
}
