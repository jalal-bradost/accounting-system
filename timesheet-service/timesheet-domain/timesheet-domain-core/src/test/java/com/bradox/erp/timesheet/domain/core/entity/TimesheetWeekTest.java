package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimesheetWeekTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    private static TimesheetWeek week() {
        return TimesheetWeek.open(new WeekId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()), UUID.randomUUID(),
                LocalDate.of(2026, 10, 3), NOW);
    }

    @Test
    void draftToSubmittedToApproved() {
        TimesheetWeek w = week();
        assertTrue(w.isEditable());
        w.submit(NOW);
        assertEquals(WeekStatus.SUBMITTED, w.getStatus());
        assertFalse(w.isEditable());
        w.approve("boss", NOW.plusSeconds(5), false);
        assertEquals(WeekStatus.APPROVED, w.getStatus());
        assertEquals("boss", w.getApprovedBy());
    }

    @Test
    void refusedWeekIsEditableAndCanBeResubmitted() {
        TimesheetWeek w = week();
        w.submit(NOW);
        w.refuse("  missing Tuesday ");
        assertEquals(WeekStatus.REFUSED, w.getStatus());
        assertEquals("missing Tuesday", w.getRefusedReason());
        assertTrue(w.isEditable());
        w.submit(NOW.plusSeconds(60));
        assertNull(w.getRefusedReason(), "resubmitting clears the old refusal");
    }

    @Test
    void refuseNeedsAReasonAndASubmittedWeek() {
        TimesheetWeek w = week();
        assertThrows(TimesheetDomainException.class, () -> w.refuse("x"), "a draft cannot be refused");
        w.submit(NOW);
        assertThrows(TimesheetDomainException.class, () -> w.refuse("  "));
    }

    @Test
    void onlySubmittedWeeksAreApprovedUnlessApprovalIsOff() {
        TimesheetWeek w = week();
        assertThrows(TimesheetDomainException.class, () -> w.approve("boss", NOW, false));
        w.approve("system", NOW, true);
        assertEquals(WeekStatus.APPROVED, w.getStatus());
    }

    @Test
    void reopenNeedsAReasonAndAnApprovedWeek() {
        TimesheetWeek w = week();
        assertThrows(TimesheetDomainException.class, () -> w.reopen("why"), "not approved yet");
        w.submit(NOW);
        w.approve("boss", NOW, false);
        assertThrows(TimesheetDomainException.class, () -> w.reopen(" "));
        w.reopen("hours were wrong");
        assertEquals(WeekStatus.DRAFT, w.getStatus());
        assertNull(w.getApprovedBy());
        assertTrue(w.isEditable());
    }

    @Test
    void lockedWeeksCannotBeEditedOrSubmitted() {
        TimesheetWeek w = week();
        w.lock();
        assertFalse(w.isEditable());
        assertThrows(TimesheetDomainException.class, () -> w.submit(NOW));
    }

    @Test
    void cannotSubmitTwice() {
        TimesheetWeek w = week();
        w.submit(NOW);
        assertThrows(TimesheetDomainException.class, () -> w.submit(NOW));
    }
}
