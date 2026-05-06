package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Read-side helpers for sales order ↔ stock move linkage. */
public interface StockMoveSalesQueryPort {

    BigDecimal sumPickedQuantityForSalesOrderLine(UUID salesOrderLineId);

    List<UUID> findPickingIdsBySalesOrderId(UUID salesOrderId);

    boolean existsNonTerminalPickingForSalesOrder(UUID salesOrderId);
}
