package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasonCommand(@NotBlank String reason) {
}
