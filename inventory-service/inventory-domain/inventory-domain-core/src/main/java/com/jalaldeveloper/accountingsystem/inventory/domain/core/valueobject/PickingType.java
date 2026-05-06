package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Operation type of a stock picking.
 * <ul>
 *   <li>{@link #INCOMING} - receipt from supplier (SUPPLIER -&gt; INTERNAL).</li>
 *   <li>{@link #OUTGOING} - delivery to customer (INTERNAL -&gt; CUSTOMER).</li>
 *   <li>{@link #INTERNAL} - transfer between two INTERNAL locations.</li>
 * </ul>
 */
public enum PickingType {
    INCOMING,
    OUTGOING,
    INTERNAL
}
