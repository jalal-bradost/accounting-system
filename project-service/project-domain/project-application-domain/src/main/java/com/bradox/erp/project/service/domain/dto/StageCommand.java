package com.bradox.erp.project.service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StageCommand(@NotBlank @Size(max = 100) String name) {
}
