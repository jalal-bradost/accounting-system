package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of running a valuation strategy on a stock move.
 *
 * @param unitCost cost actually applied to this move (for outgoing this is the weighted
 *                 average across consumed FIFO layers, or the running AVCO).
 * @param totalValue {@code unitCost * |movedQty|} (always non-negative; sign is implied by
 *                   move direction).
 * @param newAverageCost only set by AVCO strategy on incoming moves; null otherwise.
 * @param consumedLayerUpdates layers whose remaining-qty/value changed (FIFO only); the caller
 *                             must persist them along with the new layer it appends.
 */
public record ValuationResult(
        Money unitCost,
        Money totalValue,
        Money newAverageCost,
        List<StockValuationLayer> consumedLayerUpdates,
        BigDecimal absMovedQty
) {}
