package com.bradox.delin.inventory.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Read-side helpers for purchase ↔ stock move linkage. */
public interface StockMovePurchaseQueryPort {

    BigDecimal sumPickedQuantityForPurchaseOrderLine(UUID purchaseOrderLineId);

    List<UUID> findPickingIdsByPurchaseOrderId(UUID purchaseOrderId);

    /** Return pickings (OUTGOING linked to the PO), including draft returns. */
    List<UUID> findReturnPickingIdsByPurchaseOrderId(UUID purchaseOrderId);

    /** Latest DONE incoming receipt that still has net received qty to return, or empty. */
    java.util.Optional<UUID> findReturnableReceiptPickingId(UUID purchaseOrderId);

    boolean existsNonTerminalPickingForPurchaseOrder(UUID purchaseOrderId);

    /** Pickings that are neither DONE nor CANCELLED (draft/confirmed/assigned). */
    List<UUID> findNonTerminalPickingIdsByPurchaseOrderId(UUID purchaseOrderId);

    /** True when any DONE picking exists for the purchase order. */
    boolean existsDonePickingForPurchaseOrder(UUID purchaseOrderId);

    /** {@code to_refund} flag on a picking; defaults to true when picking is missing. */
    boolean isPickingToRefund(UUID pickingId);
}
