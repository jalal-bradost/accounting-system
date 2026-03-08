package com.jalaldeveloper.accountingsystem.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of scale for all monetary amounts in the accounting system.
 * Use this for debit, credit, and any money values to ensure consistency.
 */
public final class MonetaryScale {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private MonetaryScale() {}

    /**
     * Returns the given amount with the standard monetary scale applied.
     * Null-safe: returns BigDecimal.ZERO scaled if amount is null.
     */
    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        return amount.setScale(SCALE, ROUNDING_MODE);
    }
}
