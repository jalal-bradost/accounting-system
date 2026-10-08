package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.DayType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** "All Timesheets": one block per employee with a row per project/task and a total per day. */
public record TeamGridResponse(LocalDate weekStart, LocalDate weekEnd, List<LocalDate> dates,
                               List<TeamEmployee> employees) {

    public record TeamDay(LocalDate date, int loggedMinutes, int expectedMinutes, DayType dayType) {
    }

    public record TeamRow(UUID projectId, String projectName, UUID taskId, String taskName, List<Integer> minutes,
                          int totalMinutes) {
    }

    public record TeamEmployee(UUID employeeId, String employeeName, UUID weekId, WeekStatus weekStatus,
                               boolean locked, int totalMinutes, int expectedMinutes, List<TeamDay> days,
                               List<TeamRow> rows) {
    }
}
