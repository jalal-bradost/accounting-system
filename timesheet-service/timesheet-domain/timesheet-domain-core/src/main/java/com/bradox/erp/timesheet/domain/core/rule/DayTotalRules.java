package com.bradox.erp.timesheet.domain.core.rule;

/** BR-TSH-02 and D10: hard limit 24 h per employee per day, soft warning above 12 h. */
public final class DayTotalRules {

    public static final int MAX_DAY_MINUTES = 1440;
    public static final int WARN_DAY_MINUTES = 720;
    public static final int MIN_ENTRY_MINUTES = 1;

    public enum Outcome { OK, WARN, REJECT }

    private DayTotalRules() {
    }

    /** @param otherMinutes minutes already logged that day, not counting the entry being saved */
    public static Outcome check(int otherMinutes, int entryMinutes) {
        int total = otherMinutes + entryMinutes;
        if (total > MAX_DAY_MINUTES) {
            return Outcome.REJECT;
        }
        return total > WARN_DAY_MINUTES ? Outcome.WARN : Outcome.OK;
    }
}
