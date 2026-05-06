package com.jalaldeveloper.accountingsystem.purchase.domain.core;

/**
 * Stored lifecycle for {@code pur_purchase_order} (RFQ / PO). Received and billed completeness
 * are derived from line quantities.
 */
public enum PurchaseOrderState {
    DRAFT,
    SENT,
    CONFIRMED,
    CANCELLED
}
