package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;

/** BR-TSH-09: rounding applies when a timer stops. Manual entries are stored as typed. */
public final class RoundingRule {

    private RoundingRule() {
    }

    public static int apply(int minutes, int stepMinutes, RoundingMode mode) {
        if (stepMinutes <= 0 || minutes <= 0) {
            return minutes;
        }
        int rem = minutes % stepMinutes;
        if (rem == 0) {
            return minutes;
        }
        int down = minutes - rem;
        boolean up = mode == RoundingMode.UP || rem * 2 >= stepMinutes;
        return up ? down + stepMinutes : down;
    }
}
