package com.bradox.erp.timesheet.domain.core.rule;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingAndPostingRulesTest {

    private static final UUID TASK_LINE = UUID.randomUUID();
    private static final UUID RATE_LINE = UUID.randomUUID();
    private static final UUID DEFAULT_LINE = UUID.randomUUID();
    private static final UUID EMPLOYEE = UUID.randomUUID();

    @Test
    void taskLineBeatsEmployeeRateBeatsProjectDefault() {
        assertEquals(TASK_LINE, BillingResolver.resolve(TASK_LINE, Map.of(EMPLOYEE, RATE_LINE), EMPLOYEE, DEFAULT_LINE).orElseThrow());
        assertEquals(RATE_LINE, BillingResolver.resolve(null, Map.of(EMPLOYEE, RATE_LINE), EMPLOYEE, DEFAULT_LINE).orElseThrow());
        assertEquals(DEFAULT_LINE, BillingResolver.resolve(null, Map.of(), EMPLOYEE, DEFAULT_LINE).orElseThrow());
        assertEquals(DEFAULT_LINE, BillingResolver.resolve(null, Map.of(UUID.randomUUID(), RATE_LINE), EMPLOYEE, DEFAULT_LINE).orElseThrow(),
                "someone else's rate does not apply");
        assertTrue(BillingResolver.resolve(null, null, EMPLOYEE, null).isEmpty(), "billable but no order line");
    }

    @Test
    void minutesBecomeHoursOrDaysWithTwoDecimals() {
        assertEquals(new BigDecimal("1.50"), BillingQuantity.toQuantity(90, "HOURS", 480));
        assertEquals(new BigDecimal("0.33"), BillingQuantity.toQuantity(20, "HOURS", 480));
        assertEquals(new BigDecimal("1.00"), BillingQuantity.toQuantity(480, "DAYS", 480));
        assertEquals(new BigDecimal("0.50"), BillingQuantity.toQuantity(240, "DAYS", 480));
        assertEquals(new BigDecimal("0.00"), BillingQuantity.toQuantity(0, "HOURS", 480));
        assertEquals(new BigDecimal("1.06"), BillingQuantity.toQuantity(510, "DAYS", 480), "510/480 = 1.0625");
    }

    @Test
    void invoicedQuantityConvertsBackToMinutes() {
        assertEquals(90, BillingQuantity.toMinutes(new BigDecimal("1.5"), "HOURS", 480));
        assertEquals(720, BillingQuantity.toMinutes(new BigDecimal("1.5"), "DAYS", 480));
        assertEquals(0, BillingQuantity.toMinutes(null, "HOURS", 480));
    }

    @Test
    void postingGroupsByProjectAndBalances() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID acc = UUID.randomUUID();
        var result = PostingBuilder.build(List.of(
                new PostingBuilder.CostLine(a, acc, 60, new BigDecimal("1000.1234")),
                new PostingBuilder.CostLine(a, acc, 30, new BigDecimal("500.0001")),
                new PostingBuilder.CostLine(b, acc, 120, new BigDecimal("2000"))), 4);
        assertEquals(2, result.lines().size());
        BigDecimal debits = result.lines().stream().map(PostingBuilder.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, debits.compareTo(result.total()), "debits equal the credit exactly");
        assertEquals(new BigDecimal("3500.1235"), result.total());
        assertEquals(90, result.lines().stream().filter(l -> l.projectId().equals(a)).findFirst().orElseThrow().minutes());
    }

    @Test
    void roundingDifferenceLandsOnTheLargestLine() {
        UUID small = UUID.randomUUID();
        UUID big = UUID.randomUUID();
        UUID acc = UUID.randomUUID();
        // At scale 2 each of these rounds down (0.004 and 10.004), but together they round to 10.01.
        var result = PostingBuilder.build(List.of(
                new PostingBuilder.CostLine(small, acc, 1, new BigDecimal("0.004")),
                new PostingBuilder.CostLine(big, acc, 60, new BigDecimal("10.004"))), 2);
        assertEquals(new BigDecimal("10.01"), result.total());
        BigDecimal sum = result.lines().stream().map(PostingBuilder.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(result.total()));
        var bigLine = result.lines().stream().filter(l -> l.projectId().equals(big)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.01"), bigLine.amount(), "the largest line absorbs the 0.01 difference");
    }

    @Test
    void zeroOrMissingCostProducesNothing() {
        UUID acc = UUID.randomUUID();
        assertTrue(PostingBuilder.build(List.of(), 4).isEmpty());
        assertTrue(PostingBuilder.build(List.of(new PostingBuilder.CostLine(UUID.randomUUID(), acc, 60, BigDecimal.ZERO),
                new PostingBuilder.CostLine(UUID.randomUUID(), acc, 60, null)), 4).isEmpty());
    }

    private static WeekPosting pending() {
        return WeekPosting.pending(new PostingId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()), new WeekId(UUID.randomUUID()), 1,
                Instant.parse("2026-10-08T10:00:00Z"));
    }

    @Test
    void postingLifecycle() {
        WeekPosting p = pending();
        assertTrue(p.isActive());
        assertTrue(p.canProcess());
        p.markPosted(UUID.randomUUID(), java.time.LocalDate.of(2026, 10, 9), false, new BigDecimal("10"), List.of(), Instant.now());
        assertEquals(PostingStatus.POSTED, p.getStatus());
        assertFalse(p.canProcess(), "a posted week is never posted twice");
        assertThrows(TimesheetDomainException.class, () -> p.markFailed("x"));
        p.markReversed(UUID.randomUUID());
        assertFalse(p.isActive(), "a reversed posting no longer counts");
        assertThrows(TimesheetDomainException.class, () -> p.markReversed(null));
    }

    @Test
    void failedPostingsRetryUpToFiveTimes() {
        WeekPosting p = pending();
        for (int i = 0; i < WeekPosting.MAX_ATTEMPTS; i++) {
            assertTrue(p.canProcess(), "attempt " + (i + 1));
            p.markFailed("period closed");
        }
        assertFalse(p.canProcess(), "stops after 5 attempts");
        p.resetForRetry();
        assertTrue(p.canProcess(), "a manual retry starts again");
        assertThrows(TimesheetDomainException.class, () -> pending().resetForRetry(), "only failed postings are retried by hand");
    }

    @Test
    void skippedPostingStillCountsAsActiveUntilReversed() {
        WeekPosting p = pending();
        p.markSkipped();
        assertEquals(PostingStatus.SKIPPED, p.getStatus());
        assertTrue(p.isActive());
        p.markReversed(null);
        assertFalse(p.isActive());
    }
}
