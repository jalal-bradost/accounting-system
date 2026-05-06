package com.jalaldeveloper.accountingsystem.inventory.service.domain.ports.output;

import java.util.UUID;

/**
 * Optional hook after an incoming picking linked to a purchase order is validated via inventory.
 * Purchase module implements this to refresh {@code qty_received} on PO lines from stock moves.
 */
public interface PurchaseReceiveSyncPort {

    void afterIncomingPickingValidated(UUID purchaseOrderId);
}
