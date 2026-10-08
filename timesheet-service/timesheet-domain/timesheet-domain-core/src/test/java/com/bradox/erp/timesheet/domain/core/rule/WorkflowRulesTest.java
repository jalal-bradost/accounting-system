package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.timesheet.domain.core.valueobject.RoundingMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowRulesTest {

    private static final ZoneId BAGHDAD = ZoneId.of("Asia/Baghdad");   // UTC+3, no daylight saving

    @Test
    void timerInsideOneDayIsOnePiece() {
        var pieces = TimerSplit.split(Instant.parse("2026-10-08T06:00:00Z"), Instant.parse("2026-10-08T08:30:00Z"), BAGHDAD);
        assertEquals(1, pieces.size());
        assertEquals(LocalDate.of(2026, 10, 8), pieces.get(0).date());
        assertEquals(150, pieces.get(0).minutes());
    }

    @Test
    void timerPastLocalMidnightSplitsPerCalendarDate() {
        // 20:00Z is 23:00 in Baghdad; 22:00Z is 01:00 the next day: one hour on each date
        var pieces = TimerSplit.split(Instant.parse("2026-10-08T20:00:00Z"), Instant.parse("2026-10-08T22:00:00Z"), BAGHDAD);
        assertEquals(2, pieces.size());
        assertEquals(LocalDate.of(2026, 10, 8), pieces.get(0).date());
        assertEquals(60, pieces.get(0).minutes());
        assertEquals(LocalDate.of(2026, 10, 9), pieces.get(1).date());
        assertEquals(60, pieces.get(1).minutes());
    }

    @Test
    void midnightInUtcIsNotMidnightInTheCompanyZone() {
        // Crossing UTC midnight (03:00 Baghdad) must NOT split
        var pieces = TimerSplit.split(Instant.parse("2026-10-08T23:30:00Z"), Instant.parse("2026-10-09T00:30:00Z"), BAGHDAD);
        assertEquals(1, pieces.size());
        assertEquals(60, pieces.get(0).minutes());
    }

    @Test
    void multiDayRunAndEmptyRun() {
        var pieces = TimerSplit.split(Instant.parse("2026-10-08T00:00:00Z"), Instant.parse("2026-10-10T00:00:00Z"), ZoneId.of("UTC"));
        assertEquals(2, pieces.size());
        assertEquals(1440, pieces.get(0).minutes());
        assertTrue(TimerSplit.split(Instant.parse("2026-10-08T10:00:00Z"), Instant.parse("2026-10-08T10:00:20Z"), BAGHDAD).isEmpty());
        assertTrue(TimerSplit.split(Instant.parse("2026-10-08T10:00:00Z"), Instant.parse("2026-10-08T09:00:00Z"), BAGHDAD).isEmpty());
    }

    @Test
    void roundingUpAndNearest() {
        assertEquals(15, RoundingRule.apply(1, 15, RoundingMode.UP));
        assertEquals(15, RoundingRule.apply(15, 15, RoundingMode.UP));
        assertEquals(30, RoundingRule.apply(16, 15, RoundingMode.UP));
        assertEquals(0, RoundingRule.apply(7, 15, RoundingMode.NEAREST));
        assertEquals(15, RoundingRule.apply(8, 15, RoundingMode.NEAREST));
        assertEquals(37, RoundingRule.apply(37, 0, RoundingMode.UP), "step 0 means off");
    }

    @Test
    void hourlyRateFromContract() {
        assertEquals(new BigDecimal("12.5000"), CostCalculator.hourlyRate(new BigDecimal("12.5"), "hourly", 0));
        // monthly 2,600,000 over a 40 h week: monthly hours = 40 * 52 / 12 = 173.3333
        BigDecimal rate = CostCalculator.hourlyRate(new BigDecimal("2600000"), "fixed", 40 * 60);
        assertEquals(new BigDecimal("15000.0000"), rate);
        assertNull(CostCalculator.hourlyRate(new BigDecimal("1000"), "fixed", 0), "no schedule hours, no rate");
        assertNull(CostCalculator.hourlyRate(null, "fixed", 2400));
    }

    @Test
    void costOfMinutes() {
        assertEquals(new BigDecimal("22500.0000"), CostCalculator.cost(90, new BigDecimal("15000")));
        assertEquals(new BigDecimal("0.0000"), CostCalculator.cost(90, null));
        assertEquals(new BigDecimal("1.6667"), CostCalculator.cost(10, new BigDecimal("10")), "rounded half up at 4 places");
    }

    @Test
    void approverChain() {
        UUID owner = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        UUID deptManager = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        // the HR manager and the department manager, with the team approve permission
        assertTrue(ApproverRule.canApprove(manager, owner, manager, deptManager, true, false, false));
        assertTrue(ApproverRule.canApprove(deptManager, owner, manager, deptManager, true, false, false));
        // a stranger with only team approve cannot
        assertFalse(ApproverRule.canApprove(stranger, owner, manager, deptManager, true, false, false));
        // the manager without the permission cannot
        assertFalse(ApproverRule.canApprove(manager, owner, manager, deptManager, false, false, false));
        // a Timesheet Manager can approve anyone else
        assertTrue(ApproverRule.canApprove(stranger, owner, manager, deptManager, false, true, false));
    }

    @Test
    void nobodyApprovesTheirOwnWeekUnlessAllowed() {
        UUID me = UUID.randomUUID();
        assertFalse(ApproverRule.canApprove(me, me, me, null, true, false, true), "team approver never self-approves");
        assertFalse(ApproverRule.canApprove(me, me, null, null, false, true, false), "manager needs the company setting");
        assertTrue(ApproverRule.canApprove(me, me, null, null, false, true, true));
    }
}
