package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.Disposition;
import jakarta.validation.constraints.NotNull;

public record DispositionCommand(@NotNull Disposition disposition) {
}
