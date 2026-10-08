package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartTimerCommand(@NotNull UUID projectId, UUID taskId, String description) {
}
