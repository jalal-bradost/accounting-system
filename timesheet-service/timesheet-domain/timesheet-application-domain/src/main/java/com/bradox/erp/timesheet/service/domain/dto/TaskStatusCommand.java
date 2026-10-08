package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusCommand(@NotNull TaskStatus status) {
}
