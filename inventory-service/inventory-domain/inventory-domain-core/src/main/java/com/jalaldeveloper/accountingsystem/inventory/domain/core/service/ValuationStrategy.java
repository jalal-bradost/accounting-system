package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

/** Computes the unit cost and total value applied by a stock move. */
public interface ValuationStrategy {

    ValuationMethod method();

    /** Compute valuation for an incoming move (positive {@code movedQty}). */
    ValuationResult valueIncoming(ValuationContext ctx);

    /** Compute valuation for an outgoing move (negative {@code movedQty}). */
    ValuationResult valueOutgoing(ValuationContext ctx);
}
