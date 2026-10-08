package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GridResponse(UUID employeeId, String employeeName, LocalDate weekStart, LocalDate weekEnd,
                           WeekStatus weekStatus, boolean locked, boolean editable, List<GridDayResponse> days,
                           List<GridRowResponse> rows, int totalMinutes, int expectedMinutes, UUID weekId,
                           String refusedReason, boolean canSubmit, boolean canReopen) {
}
