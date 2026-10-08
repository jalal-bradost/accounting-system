package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.CopyLinesCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellResult;
import com.bradox.erp.timesheet.service.domain.dto.GridLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridResponse;
import com.bradox.erp.timesheet.service.domain.dto.TeamGridResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface GridApplicationService {

    /** {@code date} is any day of the wanted week; the week start comes from the company setting (D8). */
    GridResponse grid(CompanyId companyId, UUID employeeId, LocalDate date);

    /** "All Timesheets" (TSH-03 #8): {@code all} lists everyone (view_all), otherwise the caller's team. */
    TeamGridResponse teamGrid(CompanyId companyId, LocalDate date, boolean all);

    GridCellResult setCell(CompanyId companyId, GridCellCommand command);

    /** "Fill from attendance" (TSH-03 #12): draft entries for attended days with nothing logged; nothing happens without this call. */
    com.bradox.erp.timesheet.service.domain.dto.FillAttendanceResponse fillFromAttendance(CompanyId companyId,
            com.bradox.erp.timesheet.service.domain.dto.FillAttendanceCommand command);

    void addLine(CompanyId companyId, GridLineCommand command);

    void removeLine(CompanyId companyId, GridLineCommand command);

    /** Returns how many rows were added. */
    int copyLastWeekLines(CompanyId companyId, CopyLinesCommand command);
}
