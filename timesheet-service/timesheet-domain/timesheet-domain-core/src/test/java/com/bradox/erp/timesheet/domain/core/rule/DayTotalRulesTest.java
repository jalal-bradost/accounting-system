package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.rule.DayTotalRules.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayTotalRulesTest {

    @Test
    void boundariesAt720And1440() {
        assertEquals(Outcome.OK, DayTotalRules.check(0, 720));
        assertEquals(Outcome.WARN, DayTotalRules.check(0, 721));
        assertEquals(Outcome.WARN, DayTotalRules.check(0, 1440));
        assertEquals(Outcome.REJECT, DayTotalRules.check(0, 1441));
        assertEquals(Outcome.REJECT, DayTotalRules.check(1000, 441));
        assertEquals(Outcome.WARN, DayTotalRules.check(1000, 440));
    }
}
