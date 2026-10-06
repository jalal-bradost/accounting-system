package com.bradox.erp.domain.valueobject;

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

    /**
     * Human-readable decimal for chatter, errors, and other string UI.
     * Strips trailing zeros ({@code 85000.0000} → {@code 85000}) but keeps
     * significant fractional digits ({@code 85000.599} → {@code 85000.599}).
     * Use for both money and quantity display; keep {@link #scale} for storage/math.
     */
    public static String toDisplayString(BigDecimal amount) {
        if (amount == null) return "0";
        return amount.stripTrailingZeros().toPlainString();
    }
}
