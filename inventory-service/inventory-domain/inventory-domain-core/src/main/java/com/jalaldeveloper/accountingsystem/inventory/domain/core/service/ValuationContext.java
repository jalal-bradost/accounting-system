package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inputs/outputs handed to a {@link ValuationStrategy} when computing a single move's value.
 *
 * @param onHandQty current on-hand qty of the product across all internal locations BEFORE this
 *                  move is applied.
 * @param onHandValue current valuation across all internal locations BEFORE this move is applied.
 * @param movedQty positive for incoming, negative for outgoing (caller normalises sign).
 * @param providedUnitCost optional caller-supplied unit cost (e.g. PO price for incoming).
 *                         Required for STANDARD/AVCO incoming when no historical cost exists.
 * @param fifoCandidates ordered (oldest first) positive layers that still have remaining qty.
 *                       Only populated and consulted by FIFO strategy for outgoing moves.
 */
public record ValuationContext(
        BigDecimal onHandQty,
        Money onHandValue,
        BigDecimal movedQty,
        Money providedUnitCost,
        List<StockValuationLayer> fifoCandidates
) {}
