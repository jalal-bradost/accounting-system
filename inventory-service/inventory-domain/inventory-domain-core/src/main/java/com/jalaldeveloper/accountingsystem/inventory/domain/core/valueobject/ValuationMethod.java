package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Cost flow assumption used to value outgoing stock moves and on-hand inventory.
 * Selected per product (or inherited from category).
 */
public enum ValuationMethod {
    /** Issue at the manually-set standard cost; SVL only for in/out, no recompute on receipts. */
    STANDARD,
    /**
     * Average cost: recomputed on every receipt as
     * {@code (oldQty * oldCost + receivedQty * receivedUnitCost) / newQty}.
     * Outgoing moves consume at the running average.
     */
    AVCO,
    /**
     * First-In-First-Out: each receipt creates a positive layer; outgoing moves consume layers
     * in receipt order at their original unit cost (rounding handled per layer).
     */
    FIFO
}
