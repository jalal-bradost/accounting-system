package com.bradox.delin.accounting.service.domain.ports.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read/write sales-order line quantities needed to reconcile Stock Output → COGS.
 */
public interface SalesOrderCogsStatePort {

    record LineState(
            UUID lineId,
            UUID productId,
            String name,
            BigDecimal qtyDelivered,
            BigDecimal qtyInvoiced,
            BigDecimal qtyCogsCleared) {}

    record OrderState(UUID companyId, List<LineState> lines) {}

    Optional<OrderState> findOrder(UUID salesOrderId);

    /** Persists the new absolute {@code qty_cogs_cleared} per line id. */
    void updateQtyCogsCleared(UUID salesOrderId, Map<UUID, BigDecimal> qtyCogsClearedByLineId);
}
