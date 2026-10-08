package com.bradox.erp.timesheet.service.domain.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BulkApproveCommand(@NotEmpty List<UUID> weekIds) {
}
