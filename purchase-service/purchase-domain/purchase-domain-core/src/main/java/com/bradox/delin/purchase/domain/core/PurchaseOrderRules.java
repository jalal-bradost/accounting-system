package com.bradox.delin.purchase.domain.core;

import com.bradox.delin.domain.valueobject.DiscountMath;
import com.bradox.delin.domain.valueobject.DiscountType;

import java.math.BigDecimal;

/**
 * Pure domain rules for purchase order transitions (used by the application service).
 */
public final class PurchaseOrderRules {

    private PurchaseOrderRules() {}

    public static void ensureCanSend(PurchaseOrderState state) {
        if (state != PurchaseOrderState.DRAFT) {
            throw new PurchaseDomainException("error.purchase.sendRfqDraftOnly", new Object[] { state }, "Can only send RFQ in DRAFT state, was " + state);
        }
    }

    public static void ensureCanUpdate(PurchaseOrderState state) {
        if (state == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException(
                    "error.purchase.updateCancelledForbidden",
                    new Object[] { state },
                    "Cannot update a cancelled purchase order");
        }
    }

    public static void ensureCanConfirm(PurchaseOrderState state) {
        if (state != PurchaseOrderState.DRAFT && state != PurchaseOrderState.SENT) {
            throw new PurchaseDomainException("error.purchase.confirmFromDraftOrSent", new Object[] { state }, "Can only confirm from DRAFT or SENT, was " + state);
        }
    }

    public static void ensureCanCancel(PurchaseOrderState state) {
        if (state == PurchaseOrderState.CANCELLED) {
            throw new PurchaseDomainException("error.purchase.orderAlreadyCancelled", null, "Order already cancelled");
        }
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        return lineNet(qty, unitPrice, DiscountType.PERCENT, discountPercent);
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, DiscountType type, BigDecimal value) {
        return DiscountMath.lineNet(qty, unitPrice, type, value);
    }
}
