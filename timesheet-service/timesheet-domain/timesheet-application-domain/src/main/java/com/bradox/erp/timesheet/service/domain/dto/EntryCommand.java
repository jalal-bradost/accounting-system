package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code minutes} or {@code duration} (free text such as {@code 1:30}) gives the length; if both are present
 * {@code duration} wins. {@code employeeId} is only for a manager logging for someone else (BR-TSH-06).
 */
public record EntryCommand(UUID employeeId, LocalDate workDate, UUID projectId, UUID taskId, Integer minutes,
                           String duration, String description, Boolean billable) {
}
