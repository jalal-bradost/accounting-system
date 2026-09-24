package com.bradox.delin.domain.valueobject;

/** How a discount value is interpreted: a percentage of the gross, or an absolute amount. */
public enum DiscountType {
    PERCENT,
    FIXED;

    public static DiscountType orPercent(DiscountType type) {
        return type != null ? type : PERCENT;
    }
}
