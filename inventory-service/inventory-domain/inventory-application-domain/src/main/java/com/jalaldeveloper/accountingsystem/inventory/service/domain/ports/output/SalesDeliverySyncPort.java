package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output;

import java.util.UUID;

/**
 * Called after an OUTGOING stock picking linked to a sales order is validated,
 * so SO lines {@code qty_delivered} stay in sync (including validates from inventory-only APIs).
 */
public interface SalesDeliverySyncPort {

    void afterOutgoingPickingValidated(UUID salesOrderId);
}
