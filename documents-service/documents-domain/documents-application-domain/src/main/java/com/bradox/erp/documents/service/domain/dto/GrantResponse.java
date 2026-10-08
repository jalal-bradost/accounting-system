package com.bradox.erp.documents.service.domain.dto;

import java.util.UUID;

public record GrantResponse(String subjectType, UUID subjectId, String subjectName, String level) {
}
