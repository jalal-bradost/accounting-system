package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTemplateCommand(@NotBlank String name, @NotNull UUID documentId, String category, String defaultMessage,
                                    Integer defaultValidityDays) {
}
