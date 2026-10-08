package com.bradox.erp.timesheet.domain.core.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** TSH-07: hourly cost from a contract wage, and the cost of a number of minutes. Pure, BigDecimal only. */
public final class CostCalculator {

    private static final int SCALE = 4;

    private CostCalculator() {
    }

    /**
     * Hourly wage is used as is; a fixed (monthly) wage is divided by monthly hours, which are the weekly
     * schedule hours times 52 over 12. Returns null when the schedule has no hours.
     */
    public static BigDecimal hourlyRate(BigDecimal wage, String wageType, int weeklyMinutes) {
        if (wage == null) {
            return null;
        }
        if ("hourly".equalsIgnoreCase(wageType)) {
            return wage.setScale(SCALE, RoundingMode.HALF_UP);
        }
        if (weeklyMinutes <= 0) {
            return null;
        }
        BigDecimal monthlyMinutes = BigDecimal.valueOf(weeklyMinutes).multiply(BigDecimal.valueOf(52))
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return wage.multiply(BigDecimal.valueOf(60)).divide(monthlyMinutes, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal cost(int minutes, BigDecimal hourlyRate) {
        if (hourlyRate == null) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return hourlyRate.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60), SCALE, RoundingMode.HALF_UP);
    }
}
