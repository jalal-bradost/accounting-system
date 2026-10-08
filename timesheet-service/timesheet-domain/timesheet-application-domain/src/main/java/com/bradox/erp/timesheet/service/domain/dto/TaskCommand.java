package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** {@code projectId} is required on create and ignored on update. {@code saleLineId} needs {@code tsh.billing.manage}. */
public record TaskCommand(UUID projectId, @NotBlank String name, String description, Set<UUID> assigneeEmployeeIds,
                          Integer allocatedMinutes, LocalDate deadline, UUID saleLineId) {

    public TaskCommand(UUID projectId, String name, String description, Set<UUID> assigneeEmployeeIds,
                       Integer allocatedMinutes, LocalDate deadline) {
        this(projectId, name, description, assigneeEmployeeIds, allocatedMinutes, deadline, null);
    }
}
