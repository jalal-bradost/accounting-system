package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.service.domain.dto.CopyLinesCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridResponse;
import com.bradox.erp.timesheet.service.domain.dto.GridRowResponse;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.DayType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridApplicationServiceTest extends ServiceTestBase {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 3);   // Saturday

    private GridCellCommand cell(Project p, Task t, LocalDate d, String duration) {
        return new GridCellCommand(null, p.getId().getId(), t == null ? null : t.getId().getId(), d, null, duration);
    }

    @Test
    void weekFollowsTheCompanyStartDayAndShadesNonWorkingDays() {
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(WEEK_START, g.weekStart());
        assertEquals(LocalDate.of(2026, 10, 9), g.weekEnd());
        assertEquals(7, g.days().size());
        // Saturday and Friday are the weekend in the fake schedule
        assertEquals(DayType.NON_WORKING, g.days().get(0).dayType());
        assertEquals(DayType.WORKING, g.days().get(1).dayType());
        assertEquals(DayType.NON_WORKING, g.days().get(6).dayType());
        assertEquals(5 * 480, g.expectedMinutes());
        assertTrue(g.editable());

        var s = access.settings(COMPANY);
        s.update(null, DayOfWeek.SUNDAY, null, true, 0, 480);
        settings.save(s);
        assertEquals(LocalDate.of(2026, 10, 4), gridService.grid(COMPANY, null, TODAY).weekStart());
    }

    @Test
    void typingIntoACellCreatesUpdatesAndClearsTheEntry() {
        Project p = project("Research & Development", BillingMode.HOURLY, false);
        var created = gridService.setCell(COMPANY, cell(p, null, WEEK_START.plusDays(1), "2h"));
        assertEquals(120, created.minutes());
        assertEquals(1, entries.store.size());

        var updated = gridService.setCell(COMPANY, cell(p, null, WEEK_START.plusDays(1), "2:30"));
        assertEquals(150, updated.minutes());
        assertEquals(1, entries.store.size(), "the same entry is edited, not duplicated");

        var cleared = gridService.setCell(COMPANY, cell(p, null, WEEK_START.plusDays(1), ""));
        assertEquals(0, cleared.minutes());
        assertEquals(0, entries.store.size());
        // clearing an empty cell is a no-op
        assertEquals(0, gridService.setCell(COMPANY, cell(p, null, WEEK_START.plusDays(1), "0")).entryCount());
    }

    @Test
    void gridShowsRowsTotalsAndLoggedPerDay() {
        Project rd = project("Research & Development", BillingMode.HOURLY, false);
        Project acme = project("AGR - S00070 - Acme", BillingMode.HOURLY, true);
        Task delivery = task(acme, "Furniture Delivery");
        gridService.setCell(COMPANY, cell(rd, null, WEEK_START.plusDays(1), "8h"));
        gridService.setCell(COMPANY, cell(acme, delivery, WEEK_START.plusDays(1), "2h"));
        gridService.setCell(COMPANY, cell(acme, delivery, WEEK_START.plusDays(2), "1h"));

        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(2, g.rows().size());
        GridRowResponse first = g.rows().get(0);
        assertEquals("AGR - S00070 - Acme", first.projectName(), "rows sort by project name");
        assertEquals("Furniture Delivery", first.taskName());
        assertEquals(180, first.totalMinutes());
        assertEquals(600, g.days().get(1).loggedMinutes());
        assertEquals(660, g.totalMinutes());
    }

    @Test
    void cellWithSeveralEntriesIsReadOnlyInTheGrid() {
        Project p = project("Website", BillingMode.HOURLY, false);
        LocalDate d = WEEK_START.plusDays(1);
        entryService.log(COMPANY, log(d, p, null, 60));
        entryService.log(COMPANY, log(d, p, null, 30));
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(90, g.rows().get(0).cells().get(1).minutes());
        assertEquals(2, g.rows().get(0).cells().get(1).entryCount());
        assertEquals("error.timesheet.cellMultiple", assertThrows(TimesheetDomainException.class,
                () -> gridService.setCell(COMPANY, cell(p, null, d, "3h"))).getMessageKey());
    }

    @Test
    void addedLineShowsUpEmptyAndCanBeRemoved() {
        Project p = project("Website", BillingMode.HOURLY, false);
        gridService.addLine(COMPANY, new GridLineCommand(null, p.getId().getId(), null));
        gridService.addLine(COMPANY, new GridLineCommand(null, p.getId().getId(), null));   // idempotent
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(1, g.rows().size());
        assertTrue(g.rows().get(0).pinned());
        assertEquals(0, g.rows().get(0).totalMinutes());
        gridService.removeLine(COMPANY, new GridLineCommand(null, p.getId().getId(), null));
        assertEquals(0, gridService.grid(COMPANY, null, TODAY).rows().size());
    }

    @Test
    void cannotAddALineForAnArchivedProjectOrMismatchedTask() {
        Project a = project("A", BillingMode.HOURLY, false);
        Project b = project("B", BillingMode.HOURLY, false);
        Task t = task(a, "T");
        assertThrows(TimesheetDomainException.class, () -> gridService.addLine(COMPANY,
                new GridLineCommand(null, b.getId().getId(), t.getId().getId())));
        a.archive();
        assertThrows(TimesheetDomainException.class, () -> gridService.addLine(COMPANY,
                new GridLineCommand(null, a.getId().getId(), null)));
    }

    @Test
    void copyLastWeeksLinesPinsOnlyRowsThatAreStillOpen() {
        Project keep = project("Keep", BillingMode.HOURLY, false);
        Project gone = project("Gone", BillingMode.HOURLY, false);
        gridService.setCell(COMPANY, cell(keep, null, WEEK_START.minusDays(6), "1h"));
        gridService.setCell(COMPANY, cell(gone, null, WEEK_START.minusDays(5), "1h"));
        gone.archive();
        int added = gridService.copyLastWeekLines(COMPANY, new CopyLinesCommand(null, WEEK_START));
        assertEquals(1, added);
        assertEquals(0, gridService.copyLastWeekLines(COMPANY, new CopyLinesCommand(null, WEEK_START)), "idempotent");
        GridResponse g = gridService.grid(COMPANY, null, TODAY);
        assertEquals(1, g.rows().size());
        assertEquals("Keep", g.rows().get(0).projectName());
        assertFalse(g.rows().get(0).cells().stream().anyMatch(c -> c.minutes() > 0));
    }

    @Test
    void managersViewAnotherEmployeesGridOnlyWithPermission() {
        var other = employees.add("Beth Evans", null);
        assertThrows(com.bradox.erp.platform.security.ForbiddenException.class,
                () -> gridService.grid(COMPANY, other.id(), TODAY));
        permissions.add(TimesheetPermissions.ENTRY_VIEW_ALL);
        GridResponse g = gridService.grid(COMPANY, other.id(), TODAY);
        assertEquals("Beth Evans", g.employeeName());
        assertFalse(g.editable(), "viewing a colleague does not grant editing");
    }
}
