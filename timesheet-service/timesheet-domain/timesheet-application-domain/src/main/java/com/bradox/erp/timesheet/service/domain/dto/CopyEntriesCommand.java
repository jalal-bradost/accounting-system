package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** "Repeat yesterday" (TSH-02 #10): copies every entry of {@code fromDate} to {@code toDate}. */
public record CopyEntriesCommand(UUID employeeId, @NotNull LocalDate fromDate, @NotNull LocalDate toDate) {
}
