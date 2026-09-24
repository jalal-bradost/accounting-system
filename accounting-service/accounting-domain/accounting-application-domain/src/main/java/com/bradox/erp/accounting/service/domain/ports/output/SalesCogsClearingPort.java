package com.bradox.erp.accounting.service.domain.ports.output;

import java.util.UUID;

/**
 * Reconciles Anglo-Saxon Stock Output → COGS for a sales order.
 *
 * <p>Clears {@code min(qtyDelivered, qtyInvoiced) − qtyCogsCleared} so COGS posts at whichever
 * of delivery or invoice happens second (supports invoice-before-delivery when enabled).
 */
public interface SalesCogsClearingPort {

    void reconcileForSalesOrder(UUID salesOrderId);
}
