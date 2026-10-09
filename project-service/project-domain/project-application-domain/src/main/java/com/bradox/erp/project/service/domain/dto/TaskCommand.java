package com.bradox.erp.project.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** {@code stageId} may be left out on create: the task then goes to the first stage. */
public record TaskCommand(@NotBlank @Size(max = 255) String name, UUID stageId, String description, UUID customerPartnerId,
                          String assigneeUsername, LocalDate deadline, Integer priority) {

    public int priorityOrZero() {
        return priority == null ? 0 : priority;
    }
}
