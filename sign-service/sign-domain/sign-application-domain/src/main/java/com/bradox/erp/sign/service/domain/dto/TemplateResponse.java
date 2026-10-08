package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(UUID id, String name, String category, UUID documentId, String documentSha256, int pageCount,
                               boolean active, int defaultValidityDays, String defaultMessage, List<RoleDto> roles,
                               List<FieldDto> fields, long sentRequests, Instant createdAt, String createdBy) {
}
