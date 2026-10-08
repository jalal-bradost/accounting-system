package com.bradox.erp.repair.domain.core.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money maths for repair lines as pure functions (BR-REP-16): one rounding rule, two decimals, half up. */
public final class LaborPricing {

    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private LaborPricing() {
    }

    /** The rate that applies: the category's own, else the company default, else none (the advisor must type a price). */
    public static BigDecimal resolveRate(BigDecimal categoryRate, BigDecimal defaultRate) {
        if (categoryRate != null && categoryRate.signum() > 0) {
            return categoryRate;
        }
        return defaultRate != null && defaultRate.signum() > 0 ? defaultRate : null;
    }

    /** Flat-rate price: standard minutes / 60 x hourly rate. {@code null} when there is no rate (REP-03 #11). */
    public static BigDecimal flatRatePrice(int standardMinutes, BigDecimal hourlyRate) {
        if (hourlyRate == null) {
            return null;
        }
        return BigDecimal.valueOf(standardMinutes).multiply(hourlyRate).divide(SIXTY, 2, RoundingMode.HALF_UP);
    }

    /** qty x unit price less the percentage discount, rounded to two decimals. */
    public static BigDecimal lineTotal(BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent) {
        BigDecimal gross = qty.multiply(unitPrice);
        BigDecimal pct = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal net = gross.multiply(HUNDRED.subtract(pct)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return net;
    }
}
