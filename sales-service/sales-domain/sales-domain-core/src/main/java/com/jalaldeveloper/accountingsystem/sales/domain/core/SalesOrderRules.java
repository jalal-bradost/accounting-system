package com.jalaldeveloper.accountingsystem.sales.domain.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SalesOrderRules {

    private SalesOrderRules() {}

    public static void ensureCanSendQuotation(SalesOrderState state) {
        if (state != SalesOrderState.DRAFT) {
            throw new SalesDomainException("Can only send quotation in DRAFT state, was " + state);
        }
    }

    public static void ensureCanConfirm(SalesOrderState state) {
        if (state != SalesOrderState.DRAFT && state != SalesOrderState.QUOTATION_SENT) {
            throw new SalesDomainException("Can only confirm from DRAFT or QUOTATION_SENT, was " + state);
        }
    }

    public static void ensureCanCancel(SalesOrderState state) {
        if (state == SalesOrderState.CANCELLED) {
            throw new SalesDomainException("Order already cancelled");
        }
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal disc = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(
                disc.max(BigDecimal.ZERO).min(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return qty.multiply(unitPrice).multiply(factor);
    }
}
