package com.jalaldeveloper.accountingsystem.purchase.domain.core;

import java.math.BigDecimal;

/**
 * Pure domain rules for purchase order transitions (used by the application service).
 */
public final class PurchaseOrderRules {

    private PurchaseOrderRules() {}

    public static void ensureCanSend(PurchaseOrderState state) {
        if (state != PurchaseOrderState.DRAFT) {
            throw new PurchaseDomainException("Can only send RFQ in DRAFT state, was " + state);
        }
    }

    public static void ensureCanConfirm(PurchaseOrderState state) {
        if (state != PurchaseOrderState.DRAFT && state != PurchaseOrderState.SENT) {
            throw new PurchaseDomainException("Can only confirm from DRAFT or SENT, was " + state);
        }
    }

    public static void ensureCanCancel(PurchaseOrderState state) {
        if (state == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException("Order already cancelled");
        }
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100")).divide(new BigDecimal("100"), 8, java.math.RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }
}
