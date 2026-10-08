package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.CopyLinesCommand;
import com.bradox.erp.timesheet.service.domain.dto.FillAttendanceCommand;
import com.bradox.erp.timesheet.service.domain.dto.FillAttendanceResponse;
import com.bradox.erp.timesheet.service.domain.dto.GridCellCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridCellResult;
import com.bradox.erp.timesheet.service.domain.dto.GridLineCommand;
import com.bradox.erp.timesheet.service.domain.dto.GridResponse;
import com.bradox.erp.timesheet.service.domain.dto.TeamGridResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.GridApplicationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/timesheet/grid", produces = "application/json")
public class GridController {

    private final GridApplicationService grid;

    public GridController(GridApplicationService grid) {
        this.grid = grid;
    }

    /** {@code date} is any day of the wanted week; omit it for the current week. */
    @GetMapping
    @RequiresPermission("tsh.entry.own")
    public GridResponse get(@CurrentCompany CompanyId companyId,
                            @RequestParam(required = false) UUID employeeId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return grid.grid(companyId, employeeId, date);
    }

    /** "All Timesheets": {@code all=true} lists everyone (view_all), otherwise the caller's team. */
    @GetMapping("/team")
    @RequiresPermission("tsh.entry.own")
    public TeamGridResponse team(@CurrentCompany CompanyId companyId,
                                 @RequestParam(defaultValue = "false") boolean all,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return grid.teamGrid(companyId, date, all);
    }

    @PutMapping("/cell")
    @RequiresPermission("tsh.entry.own")
    public GridCellResult setCell(@CurrentCompany CompanyId companyId, @Valid @RequestBody GridCellCommand command) {
        return grid.setCell(companyId, command);
    }

    @PostMapping("/lines")
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<Void> addLine(@CurrentCompany CompanyId companyId, @Valid @RequestBody GridLineCommand command) {
        grid.addLine(companyId, command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lines/remove")
    @RequiresPermission("tsh.entry.own")
    public ResponseEntity<Void> removeLine(@CurrentCompany CompanyId companyId, @Valid @RequestBody GridLineCommand command) {
        grid.removeLine(companyId, command);
        return ResponseEntity.noContent().build();
    }

    /** "Fill from attendance": draft entries for attended days with nothing logged. Never automatic (D16). */
    @PostMapping("/fill-from-attendance")
    @RequiresPermission("tsh.entry.own")
    public FillAttendanceResponse fillFromAttendance(@CurrentCompany CompanyId companyId,
                                                     @Valid @RequestBody FillAttendanceCommand command) {
        return grid.fillFromAttendance(companyId, command);
    }

    @PostMapping("/copy-last-week")
    @RequiresPermission("tsh.entry.own")
    public Map<String, Integer> copyLastWeek(@CurrentCompany CompanyId companyId, @Valid @RequestBody CopyLinesCommand command) {
        return Map.of("added", grid.copyLastWeekLines(companyId, command));
    }
}
