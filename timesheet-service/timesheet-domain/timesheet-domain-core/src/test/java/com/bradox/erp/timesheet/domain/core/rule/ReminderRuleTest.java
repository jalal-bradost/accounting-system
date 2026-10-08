package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.rule.ReminderRule.WeekFact;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderRuleTest {

    private static final LocalDate W1 = LocalDate.of(2026, 9, 12);
    private static final LocalDate W2 = LocalDate.of(2026, 9, 19);
    private static final LocalDate W3 = LocalDate.of(2026, 9, 26);

    @Test
    void submittedAndApprovedWeeksAreNotMissing() {
        assertTrue(ReminderRule.missingWeeks(List.of(
                new WeekFact(W1, WeekStatus.SUBMITTED, true, 2400),
                new WeekFact(W2, WeekStatus.APPROVED, true, 2400))).isEmpty());
    }

    @Test
    void draftAndRefusedWeeksWithTimeAreMissing() {
        assertEquals(List.of(W1, W2), ReminderRule.missingWeeks(List.of(
                new WeekFact(W2, WeekStatus.REFUSED, true, 2400),
                new WeekFact(W1, WeekStatus.DRAFT, true, 0))));
    }

    @Test
    void aWeekWithNoRowIsMissingOnlyIfHoursWereExpected() {
        assertEquals(List.of(W2), ReminderRule.missingWeeks(List.of(
                new WeekFact(W1, null, false, 0),
                new WeekFact(W2, null, false, 2400))));
    }

    @Test
    void anEmptyWeekWithNothingExpectedIsLeftAlone() {
        assertTrue(ReminderRule.missingWeeks(List.of(new WeekFact(W3, WeekStatus.DRAFT, false, 0))).isEmpty(),
                "full leave: nothing logged and nothing expected");
    }

    @Test
    void resultIsOldestFirst() {
        assertEquals(List.of(W1, W2, W3), ReminderRule.missingWeeks(List.of(
                new WeekFact(W3, null, true, 0), new WeekFact(W1, null, true, 0), new WeekFact(W2, null, true, 0))));
    }
}
