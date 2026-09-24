package com.bradox.erp.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Discount arithmetic shared by purchase, sales and accounting.
 *
 * <p>Fixed discounts are applied as exact amounts and never routed through a percentage, so large
 * currency values (IQD) do not drift by rounding the percent to 4 decimals.
 */
public final class DiscountMath {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** Money scale used across order/bill/invoice amounts. */
    public static final int MONEY_SCALE = 4;

    private DiscountMath() {}

    public static BigDecimal clampPercent(BigDecimal percent) {
        if (percent == null || percent.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return percent.compareTo(HUNDRED) > 0 ? HUNDRED : percent;
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    /** Discount amount deducted from {@code gross}, never more than the gross itself. */
    public static BigDecimal discountAmount(BigDecimal gross, DiscountType type, BigDecimal value) {
        BigDecimal base = nonNegative(gross);
        if (DiscountType.orPercent(type) == DiscountType.FIXED) {
            return nonNegative(value).min(base);
        }
        return base.multiply(clampPercent(value).divide(HUNDRED, 8, RoundingMode.HALF_UP));
    }

    /** {@code gross} minus the discount, floored at zero. */
    public static BigDecimal net(BigDecimal gross, DiscountType type, BigDecimal value) {
        BigDecimal base = nonNegative(gross);
        return base.subtract(discountAmount(base, type, value)).max(BigDecimal.ZERO);
    }

    public static BigDecimal lineNet(BigDecimal qty, BigDecimal unitPrice, DiscountType type, BigDecimal value) {
        if (qty == null || unitPrice == null || qty.signum() <= 0 || unitPrice.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return net(qty.multiply(unitPrice), type, value);
    }

    /**
     * Equivalent percentage for a discount, used only for reporting columns that still read a
     * percent. Money math must use {@link #net} so fixed amounts stay exact.
     */
    public static BigDecimal effectivePercent(BigDecimal gross, DiscountType type, BigDecimal value) {
        if (DiscountType.orPercent(type) == DiscountType.PERCENT) {
            return clampPercent(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal base = nonNegative(gross);
        if (base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        return discountAmount(base, type, value)
                .multiply(HUNDRED)
                .divide(base, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Discount for a document line derived from an order line: the order-level discount is spread
     * across lines by the ratio it removes from the order subtotal. Returned as a FIXED amount so
     * the billed/invoiced total matches the order exactly.
     *
     * @param lineGross        gross of the document line (billed qty × unit price)
     * @param lineNetAfterLine line net after its own discount
     * @param orderSubtotal    order subtotal before the order-level discount
     * @param orderDiscount    order-level discount amount
     */
    public static BigDecimal documentLineDiscountAmount(BigDecimal lineGross,
                                                        BigDecimal lineNetAfterLine,
                                                        BigDecimal orderSubtotal,
                                                        BigDecimal orderDiscount) {
        BigDecimal gross = nonNegative(lineGross);
        BigDecimal net = nonNegative(lineNetAfterLine);
        BigDecimal subtotal = nonNegative(orderSubtotal);
        BigDecimal orderDisc = nonNegative(orderDiscount);
        if (subtotal.signum() > 0 && orderDisc.signum() > 0) {
            BigDecimal factor = subtotal.subtract(orderDisc).max(BigDecimal.ZERO)
                    .divide(subtotal, 12, RoundingMode.HALF_UP);
            net = net.multiply(factor);
        }
        return gross.subtract(net).max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
