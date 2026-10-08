package com.bradox.erp.sign.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ExtendCommand(@NotNull Instant expiresAt) {
}
