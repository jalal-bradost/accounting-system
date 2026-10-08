package com.bradox.erp.timesheet.service.domain.dto;

import java.util.List;

/** {@code days} are the dates that got an entry. Days that already had time, or that are not working days, are left alone. */
public record FillAttendanceResponse(int created, int minutes, List<java.time.LocalDate> days) {
}
