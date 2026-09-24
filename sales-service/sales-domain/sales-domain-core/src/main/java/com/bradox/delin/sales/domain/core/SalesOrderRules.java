package com.bradox.delin.sales.domain.core;

import com.bradox.delin.domain.valueobject.DiscountMath;
import com.bradox.delin.domain.valueobject.DiscountType;

import java.math.BigDecimal;

public final class SalesOrderRules {

    private SalesOrderRules() {}

    public static void ensureCanSendQuotation(SalesOrderState state) {
        if (state != SalesOrderState.DRAFT) {
            throw new SalesDomainException("error.sales.sendQuotationDraftOnly", new Object[] { state }, "Can only send quotation in DRAFT state, was " + state);
        }
    }

    public static void ensureCanUpdate(SalesOrderState state) {
        if (state == SalesOrderState.CANCELLED) {
            throw new SalesDomainException(
                    "error.sales.updateCancelledForbidden",
                    new Object[] { state },
                    "Cannot update a cancelled sales order");
        }
    }

    public static void ensureCanConfirm(SalesOrderState state) {
        if (state != SalesOrderState.DRAFT && state != SalesOrderState.QUOTATION_SENT) {
            throw new SalesDomainException("error.sales.confirmFromDraftOrQuotation", new Object[] { state }, "Can only confirm from DRAFT or QUOTATION_SENT, was " + state);
        }
    }

    public static void ensureCanCancel(SalesOrderState state) {
        if (state == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("error.sales.orderAlreadyCancelled", null, "Order already cancelled");
        }
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        return lineNet(qty, unitPrice, DiscountType.PERCENT, discountPercent);
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, DiscountType type, BigDecimal value) {
        return DiscountMath.lineNet(qty, unitPrice, type, value);
    }
}
