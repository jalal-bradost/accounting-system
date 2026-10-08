package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GridLineCommand(UUID employeeId, @NotNull UUID projectId, UUID taskId) {
}
