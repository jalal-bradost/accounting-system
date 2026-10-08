package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** {@code facetId} is required on create and ignored on update. */
public record TagCommand(UUID facetId, @NotBlank String name, String color, Integer sequence) {
}
