package com.bradox.erp.timesheet.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record TimerResponse(UUID projectId, String projectName, UUID taskId, String taskName, String description,
                            Instant startedAt, long elapsedSeconds) {
}
