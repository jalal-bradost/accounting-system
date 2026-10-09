package com.bradox.erp.project.service.domain.dto;

import java.util.UUID;

public record StageResponse(UUID id, UUID projectId, String name, int sequence) {
}
