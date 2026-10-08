package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.service.domain.dto.FillAttendanceCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridResponse;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TSH-03 #11 attendance hint and #12 fill, and the accounting integrity check, over fakes. */
class AttendanceAndIntegrityTest extends ServiceTestBase {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 3);
    private Project project;

    @BeforeEach
    void setUpProject() {
        project = project("Website", BillingMode.HOURLY, false);
        permissions.add(TimesheetPermissions.POSTING_MANAGE);
    }

    @Test
    void attendanceShowsOnTheGridButIsNeverCountedAsLoggedTime() {
        attendance.minutes.put(WEEK_START.plusDays(1), 500);
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        var day = g.days().get(1);
        assertEquals(500, day.attendedMinutes());
        assertEquals(0, day.loggedMinutes());
        assertEquals(0, g.totalMinutes());
    }

    @Test
    void fillCreatesDraftEntriesOnlyForWorkedDaysWithNothingLoggedCappedAtExpected() {
        LocalDate mon = WEEK_START.plusDays(1), tue = WEEK_START.plusDays(2), wed = WEEK_START.plusDays(3);
        attendance.minutes.put(mon, 600);                       // more than the 8h expected: capped
        attendance.minutes.put(tue, 300);
        attendance.minutes.put(wed, 400);                       // already has time: left alone
        attendance.minutes.put(WEEK_START.plusDays(6), 200);    // Friday, non-working: left alone
        entryService.log(COMPANY, log(wed, project, null, 60));

        var r = gridService.fillFromAttendance(COMPANY, new FillAttendanceCommand(null, TODAY, project.getId().getId(), null));

        assertEquals(2, r.created());
        assertEquals(480 + 300, r.minutes());
        assertEquals(List.of(mon, tue), r.days());
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(480, g.days().get(1).loggedMinutes());
        assertEquals(60, g.days().get(3).loggedMinutes());
        assertEquals(0, g.days().get(6).loggedMinutes());
        // pressing it again fills nothing more
        assertEquals(0, gridService.fillFromAttendance(COMPANY, new FillAttendanceCommand(null, TODAY, project.getId().getId(), null)).created());
    }

    @Test
    void integrityIsCleanWhenNothingIsPostedOrApproved() {
        entryService.log(COMPANY, log(TODAY, project, null, 120));
        weekService.submit(COMPANY, new SubmitWeekCommand(null, TODAY));
        assertTrue(postingService.integrity(COMPANY).ok());
    }
}
