package com.bradox.erp.inventory.domain.core.valueobject;

/**
 * Soft-hold ownership lifecycle for stock claims.
 * <ul>
 *   <li>{@link #CART} — unsaved / in-progress cart session (short TTL).</li>
 *   <li>{@link #DRAFT} — saved order draft (longer TTL).</li>
 *   <li>{@link #SALES_ORDER} — submitted SO awaiting confirm/assign (longer TTL).</li>
 * </ul>
 */
public enum StockHoldOwnerType {
    CART,
    DRAFT,
    SALES_ORDER
}
