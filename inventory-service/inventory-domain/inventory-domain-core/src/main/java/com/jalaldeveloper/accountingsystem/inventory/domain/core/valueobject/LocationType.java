package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Categorises a {@code StockLocation}. Internal locations contribute to on-hand quantity and
 * valuation; external (CUSTOMER, SUPPLIER) and virtual (TRANSIT, INVENTORY_LOSS) locations
 * are the source/destination on the other side of receipts/deliveries/adjustments.
 */
public enum LocationType {
    /** Stock owned by the company (e.g. WH/Stock, WH/Input, WH/Output). */
    INTERNAL,
    /** Vendor location used as source of incoming receipts. */
    SUPPLIER,
    /** Customer location used as destination of outgoing deliveries. */
    CUSTOMER,
    /** Goods in transit between two internal locations. */
    TRANSIT,
    /** Virtual counterparty for inventory adjustments / scrap. */
    INVENTORY_LOSS,
    /** Production location (manufacturing input/output). */
    PRODUCTION,
    /** A view-only grouping node in the location tree, not a physical place. */
    VIEW
}
