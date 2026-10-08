package com.bradox.erp.repair.service.domain.dto;

import com.bradox.erp.repair.domain.core.valueobject.LineType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PackageCommand(String code, @NotBlank String name, String category, boolean active, List<Line> lines) {

    public record Line(LineType type, UUID productId, UUID laborGuideId, String description, BigDecimal qty, Integer standardMinutes) {
    }
}
