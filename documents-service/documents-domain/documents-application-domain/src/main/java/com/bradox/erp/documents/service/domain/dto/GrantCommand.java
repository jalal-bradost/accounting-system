package com.bradox.erp.documents.service.domain.dto;

import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantCommand(@NotNull SubjectType subjectType, @NotNull UUID subjectId, @NotNull AccessLevel level) {
}
