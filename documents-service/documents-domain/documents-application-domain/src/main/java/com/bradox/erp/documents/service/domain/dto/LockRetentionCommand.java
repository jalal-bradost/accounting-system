package com.bradox.erp.documents.service.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LockRetentionCommand(@NotNull LocalDate until, String reason) {
}
