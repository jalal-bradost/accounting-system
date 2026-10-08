package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record SetTagsCommand(@NotNull Set<UUID> tagIds) {
}
