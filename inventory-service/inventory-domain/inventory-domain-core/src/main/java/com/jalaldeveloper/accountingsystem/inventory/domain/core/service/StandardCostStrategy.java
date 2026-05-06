package com.jalaldeveloper.accountingsystem.inventory.domain.core.service;

import com.jalaldeveloper.accountingsystem.domain.valueobject.Money;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.ValuationMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Always values moves at the product's manually-set standard cost
 * (carried in {@link ValuationContext#providedUnitCost()}). Receipts do not change the cost;
 * outgoing moves consume at the standard. Variances vs purchase price are out of scope here
 * (would post a price-difference JE separately if implemented).
 */
public class StandardCostStrategy implements ValuationStrategy {

    @Override
    public ValuationMethod method() {
        return ValuationMethod.STANDARD;
    }

    @Override
    public ValuationResult valueIncoming(ValuationContext ctx) {
        return value(ctx);
    }

    @Override
    public ValuationResult valueOutgoing(ValuationContext ctx) {
        return value(ctx);
    }

    private ValuationResult value(ValuationContext ctx) {
        if (ctx.providedUnitCost() == null) {
            throw new InventoryDomainException(
                    "STANDARD valuation requires a unit cost (product.standardCost) on the move");
        }
        BigDecimal absQty = ctx.movedQty().abs();
        Money total = new Money(ctx.providedUnitCost().getAmount().multiply(absQty));
        return new ValuationResult(ctx.providedUnitCost(), total, null, List.of(), absQty);
    }
}
