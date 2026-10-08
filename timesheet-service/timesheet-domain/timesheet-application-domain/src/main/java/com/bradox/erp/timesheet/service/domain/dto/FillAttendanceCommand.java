package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** Fills the week that contains {@code date} from attendance, onto one project and optional task (TSH-03 #12). */
public record FillAttendanceCommand(UUID employeeId, @NotNull LocalDate date, @NotNull UUID projectId, UUID taskId) {
}
