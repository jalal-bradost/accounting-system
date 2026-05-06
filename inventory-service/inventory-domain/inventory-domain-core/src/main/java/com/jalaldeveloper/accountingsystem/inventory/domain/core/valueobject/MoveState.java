package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Lifecycle of an individual {@code StockMove}. Tightly coupled with the parent picking state
 * but tracked independently to support partial validation / backorders.
 */
public enum MoveState {
    DRAFT,
    CONFIRMED,
    /** Reservation succeeded for the requested qty. */
    ASSIGNED,
    /** Reservation succeeded for less than requested (caller may split or backorder). */
    PARTIALLY_ASSIGNED,
    DONE,
    CANCELLED
}
