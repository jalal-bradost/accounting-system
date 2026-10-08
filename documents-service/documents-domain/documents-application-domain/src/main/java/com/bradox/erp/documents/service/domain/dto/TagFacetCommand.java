package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record TagFacetCommand(@NotBlank String name, Integer sequence) {
}
