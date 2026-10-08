package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** "Copy last week's lines" (TSH-03 #6): pins the previous week's rows onto {@code weekStart}'s grid. */
public record CopyLinesCommand(UUID employeeId, @NotNull LocalDate weekStart) {
}
