package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddLinkCommand(@NotBlank String modelName, @NotNull UUID recordId) {
}
