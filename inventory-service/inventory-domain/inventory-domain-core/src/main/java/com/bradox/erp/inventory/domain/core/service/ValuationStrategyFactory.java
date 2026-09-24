package com.bradox.erp.inventory.domain.core.service;

import com.bradox.erp.inventory.domain.core.exception.InventoryDomainException;
import com.bradox.erp.inventory.domain.core.valueobject.ValuationMethod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the {@link ValuationStrategy} for a given {@link ValuationMethod}. Lives in the
 * domain so the application layer can remain free of strategy plumbing details.
 */
public class ValuationStrategyFactory {

    private final Map<ValuationMethod, ValuationStrategy> strategies = new EnumMap<>(ValuationMethod.class);

    public ValuationStrategyFactory() {
        strategies.put(ValuationMethod.STANDARD, new StandardCostStrategy());
        strategies.put(ValuationMethod.AVCO, new AvcoStrategy());
        strategies.put(ValuationMethod.FIFO, new FifoStrategy());
    }

    public ValuationStrategy forMethod(ValuationMethod method) {
        ValuationStrategy s = strategies.get(method);
        if (s == null) {
            throw new InventoryDomainException("error.inventory.noValuationStrategy", new Object[] { method }, "No valuation strategy registered for " + method);
        }
        return s;
    }
}
