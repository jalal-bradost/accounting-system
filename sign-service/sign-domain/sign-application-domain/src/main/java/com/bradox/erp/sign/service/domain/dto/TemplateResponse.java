package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(UUID id, String name, UUID documentId, int pageCount, List<FieldDto> fields, long signedCount,
                               Instant createdAt) {
}
