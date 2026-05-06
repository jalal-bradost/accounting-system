package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Average Cost (weighted moving average). Computes:
 *
 * <ul>
 *   <li>Incoming: {@code newAvg = (oldQty * oldAvg + receivedQty * receivedUnitCost) / (oldQty + receivedQty)}.
 *       Returns {@code newAverageCost} so the caller can persist it on the product.</li>
 *   <li>Outgoing: values at the current running average ({@code onHandValue / onHandQty}).
 *       Outgoing moves do not change the running average.</li>
 * </ul>
 */
public class AvcoStrategy implements ValuationStrategy {

    private static final int COST_SCALE = 4;

    @Override
    public ValuationMethod method() {
        return ValuationMethod.AVCO;
    }

    @Override
    public ValuationResult valueIncoming(ValuationContext ctx) {
        if (ctx.providedUnitCost() == null) {
            throw new InventoryDomainException(
                    "AVCO incoming move requires a unit cost (e.g. PO price)");
        }
        BigDecimal absQty = ctx.movedQty().abs();
        Money totalReceiptValue = new Money(ctx.providedUnitCost().getAmount().multiply(absQty));
        BigDecimal newQty = ctx.onHandQty().add(absQty);
        BigDecimal newValue = ctx.onHandValue().getAmount().add(totalReceiptValue.getAmount());
        Money newAvg;
        if (newQty.signum() <= 0) {
            newAvg = ctx.providedUnitCost();
        } else {
            newAvg = new Money(newValue.divide(newQty, COST_SCALE, RoundingMode.HALF_UP));
        }
        return new ValuationResult(ctx.providedUnitCost(), totalReceiptValue, newAvg, List.of(), absQty);
    }

    @Override
    public ValuationResult valueOutgoing(ValuationContext ctx) {
        BigDecimal absQty = ctx.movedQty().abs();
        Money runningAvg;
        if (ctx.onHandQty().signum() <= 0) {
            // Negative-stock or first move out: fall back to the provided unit cost (or zero).
            runningAvg = ctx.providedUnitCost() != null ? ctx.providedUnitCost() : Money.ZERO;
        } else {
            runningAvg = new Money(ctx.onHandValue().getAmount()
                    .divide(ctx.onHandQty(), COST_SCALE, RoundingMode.HALF_UP));
        }
        Money totalValue = new Money(runningAvg.getAmount().multiply(absQty));
        return new ValuationResult(runningAvg, totalValue, null, List.of(), absQty);
    }
}
