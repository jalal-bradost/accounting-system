package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** Sets the whole cell to one length. Zero (or blank {@code duration}) removes the entry. */
public record GridCellCommand(UUID employeeId, @NotNull UUID projectId, UUID taskId, @NotNull LocalDate workDate,
                              Integer minutes, String duration) {
}
