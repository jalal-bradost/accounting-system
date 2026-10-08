package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** Submits the week that contains {@code date} for {@code employeeId} (default: the caller). */
public record SubmitWeekCommand(UUID employeeId, @NotNull LocalDate date) {
}
