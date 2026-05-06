package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.entity.StockValuationLayer;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * First-In-First-Out. Receipts are valued at the supplied unit cost (PO price) and the
 * created layer is the source of truth for future outgoing moves. Outgoing moves consume
 * existing positive layers in receipt order; their unit cost is the weighted average of the
 * consumed layers' original unit costs.
 *
 * <p>Negative stock fallback: if FIFO candidates are insufficient, the remaining qty is
 * valued at the provided unit cost (or zero). The accounting team should track such "FIFO
 * holes" and reconcile them when stock comes back positive.
 */
public class FifoStrategy implements ValuationStrategy {

    private static final int COST_SCALE = 4;

    @Override
    public ValuationMethod method() {
        return ValuationMethod.FIFO;
    }

    @Override
    public ValuationResult valueIncoming(ValuationContext ctx) {
        if (ctx.providedUnitCost() == null) {
            throw new InventoryDomainException("FIFO incoming move requires a unit cost (e.g. PO price)");
        }
        BigDecimal absQty = ctx.movedQty().abs();
        Money totalValue = new Money(ctx.providedUnitCost().getAmount().multiply(absQty));
        return new ValuationResult(ctx.providedUnitCost(), totalValue, null, List.of(), absQty);
    }

    @Override
    public ValuationResult valueOutgoing(ValuationContext ctx) {
        BigDecimal absQty = ctx.movedQty().abs();
        BigDecimal remaining = absQty;
        BigDecimal totalValue = BigDecimal.ZERO;
        List<StockValuationLayer> updated = new ArrayList<>();

        if (ctx.fifoCandidates() != null) {
            for (StockValuationLayer layer : ctx.fifoCandidates()) {
                if (remaining.signum() <= 0) break;
                if (layer.getRemainingQuantity() == null || layer.getRemainingQuantity().signum() <= 0) continue;
                StockValuationLayer.ConsumptionResult cons = layer.consume(remaining);
                if (cons.quantity().signum() <= 0) continue;
                remaining = remaining.subtract(cons.quantity());
                totalValue = totalValue.add(cons.value().getAmount());
                updated.add(layer);
            }
        }

        if (remaining.signum() > 0) {
            // Negative-stock hole: value the rest at the provided unit cost (or zero).
            BigDecimal fallback = ctx.providedUnitCost() != null
                    ? ctx.providedUnitCost().getAmount()
                    : BigDecimal.ZERO;
            totalValue = totalValue.add(fallback.multiply(remaining));
        }

        Money unitCost = absQty.signum() > 0
                ? new Money(totalValue.divide(absQty, COST_SCALE, RoundingMode.HALF_UP))
                : Money.ZERO;
        return new ValuationResult(unitCost, new Money(totalValue), null, updated, absQty);
    }
}
