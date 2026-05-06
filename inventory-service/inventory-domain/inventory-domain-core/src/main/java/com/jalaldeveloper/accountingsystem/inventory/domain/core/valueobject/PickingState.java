package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Lifecycle of a {@code StockPicking} (mirrors Odoo's stock.picking states).
 *
 * <pre>
 *  DRAFT --confirm--&gt; CONFIRMED --assign--&gt; ASSIGNED --validate--&gt; DONE
 *    \________________ \_________________ \____________________/
 *                                                              \-cancel-&gt; CANCELLED
 * </pre>
 */
public enum PickingState {
    /** Editable, not yet committed to schedule a move. */
    DRAFT,
    /** Confirmed and waiting on stock availability for at least one move. */
    CONFIRMED,
    /** All moves have reserved their required quantity (or are explicitly partially reserved). */
    ASSIGNED,
    /** Validated; quants have been mutated and SVLs / journal entries posted. */
    DONE,
    /** Cancelled before validation; no stock or accounting effect. */
    CANCELLED
}
