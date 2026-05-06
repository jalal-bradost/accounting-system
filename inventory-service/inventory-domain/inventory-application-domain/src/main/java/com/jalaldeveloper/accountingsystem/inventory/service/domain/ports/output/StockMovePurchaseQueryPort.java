package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Read-side helpers for purchase ↔ stock move linkage. */
public interface StockMovePurchaseQueryPort {

    BigDecimal sumPickedQuantityForPurchaseOrderLine(UUID purchaseOrderLineId);

    List<UUID> findPickingIdsByPurchaseOrderId(UUID purchaseOrderId);

    boolean existsNonTerminalPickingForPurchaseOrder(UUID purchaseOrderId);
}
