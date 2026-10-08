package com.bradox.erp.timesheet.domain.core.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** BR-TSH-13: minutes to a sales quantity in the line's unit (hours, or days at {@code minutesPerDay}), 2 decimals. */
public final class BillingQuantity {

    public static final String HOURS = "HOURS";
    public static final String DAYS = "DAYS";

    private BillingQuantity() {
    }

    /** Convert the SUM of minutes once, never entry by entry, so rounding cannot drift. */
    public static BigDecimal toQuantity(long minutes, String unit, int minutesPerDay) {
        BigDecimal divisor = DAYS.equals(unit) ? BigDecimal.valueOf(Math.max(1, minutesPerDay)) : BigDecimal.valueOf(60);
        return BigDecimal.valueOf(minutes).divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /** The reverse, for allocating an invoiced quantity back onto entries. */
    public static long toMinutes(BigDecimal quantity, String unit, int minutesPerDay) {
        if (quantity == null) {
            return 0;
        }
        BigDecimal factor = DAYS.equals(unit) ? BigDecimal.valueOf(Math.max(1, minutesPerDay)) : BigDecimal.valueOf(60);
        return quantity.multiply(factor).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
