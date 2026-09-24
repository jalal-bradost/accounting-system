package com.bradox.delin.inventory.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-side helpers for sales order ↔ stock move linkage. */
public interface StockMoveSalesQueryPort {

    /**
     * Net delivered valuation for a sales order line (DONE outgoing minus DONE returns),
     * used to clear Stock Output → COGS at invoice time.
     */
    record NetDeliveredCost(UUID productId, BigDecimal quantity, BigDecimal value) {}

    BigDecimal sumPickedQuantityForSalesOrderLine(UUID salesOrderLineId);

    /**
     * Net delivered qty/value at the unit costs recorded on done stock moves.
     * Empty when there is no valued delivery for the line.
     */
    Optional<NetDeliveredCost> netDeliveredCostForSalesOrderLine(UUID salesOrderLineId);

    /**
     * Gross outgoing delivery qty/value (ignores returns). Used for credit-note COGS
     * reversal after goods have already been returned.
     */
    Optional<NetDeliveredCost> grossDeliveredCostForSalesOrderLine(UUID salesOrderLineId);

    List<UUID> findPickingIdsBySalesOrderId(UUID salesOrderId);

    /** Return pickings (INCOMING linked to the SO), including draft returns. */
    List<UUID> findReturnPickingIdsBySalesOrderId(UUID salesOrderId);

    /** Latest DONE outgoing delivery that still has net delivered qty to return, or empty. */
    Optional<UUID> findReturnableDeliveryPickingId(UUID salesOrderId);

    boolean existsNonTerminalPickingForSalesOrder(UUID salesOrderId);

    /** Whether a validated return picking should trigger a customer credit note. Defaults true if unknown. */
    boolean isPickingToRefund(UUID pickingId);
}
