package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReplaceDocumentCommand(@NotNull UUID documentId) {
}
