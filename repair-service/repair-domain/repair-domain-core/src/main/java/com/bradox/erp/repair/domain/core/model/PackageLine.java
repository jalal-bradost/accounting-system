package com.bradox.erp.repair.domain.core.model;

import com.bradox.erp.repair.domain.core.valueobject.LineType;

import java.math.BigDecimal;
import java.util.UUID;

public record PackageLine(UUID id, int sequence, LineType type, UUID productId, UUID laborGuideId, String description,
                          BigDecimal qty, Integer standardMinutes) {
}
