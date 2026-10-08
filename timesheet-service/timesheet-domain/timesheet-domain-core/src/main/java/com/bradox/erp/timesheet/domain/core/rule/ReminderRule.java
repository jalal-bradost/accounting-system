package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Which finished weeks an employee still owes (TSH-05 #8). A week is missing when it was not submitted or approved AND
 * there was something to submit: time was logged, or the work schedule expected hours. Someone on leave for the whole
 * week, with nothing logged and nothing expected, is not nagged.
 */
public final class ReminderRule {

    /** {@code status} is null when the employee has no row for that week at all. */
    public record WeekFact(LocalDate weekStart, WeekStatus status, boolean hasEntries, int expectedMinutes) {
    }

    private ReminderRule() {
    }

    public static List<LocalDate> missingWeeks(List<WeekFact> facts) {
        return facts.stream()
                .filter(f -> f.status() != WeekStatus.SUBMITTED && f.status() != WeekStatus.APPROVED)
                .filter(f -> f.hasEntries() || f.expectedMinutes() > 0)
                .map(WeekFact::weekStart)
                .sorted()
                .toList();
    }
}
