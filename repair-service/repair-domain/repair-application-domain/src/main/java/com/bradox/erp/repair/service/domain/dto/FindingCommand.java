package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FindingCommand(@NotNull Severity severity, @NotBlank String description, String cause, String recommendedAction,
                             boolean customerVisible) {
}
