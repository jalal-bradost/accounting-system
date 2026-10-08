package com.bradox.erp.repair.domain.core.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LaborCategory(UUID id, UUID companyId, String name, BigDecimal hourlyRate, boolean active) {
}
