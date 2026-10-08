package com.bradox.erp.documents.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record RecordLinkResponse(String modelName, UUID recordId, String label, Instant createdAt, String createdBy) {
}
