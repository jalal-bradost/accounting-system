package com.bradox.erp.project.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Drop a task into {@code stageId} at position {@code index} (0 is the top of the column). */
public record MoveTaskCommand(@NotNull UUID stageId, int index) {
}
