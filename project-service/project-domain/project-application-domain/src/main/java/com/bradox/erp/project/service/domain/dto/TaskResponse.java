package com.bradox.erp.project.service.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(UUID id, UUID projectId, String projectName, UUID stageId, String stageName, String name,
                           String description, UUID customerPartnerId, String assigneeUsername, LocalDate deadline,
                           int priority, int sequence, Instant createdAt) {
}
