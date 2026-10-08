package com.bradox.erp.repair.domain.core.model;

import com.bradox.erp.repair.domain.core.valueobject.Severity;

import java.time.Instant;
import java.util.UUID;

public record Finding(UUID id, UUID companyId, UUID orderId, Severity severity, String description, String cause,
                      String recommendedAction, boolean customerVisible, Instant createdAt, String createdBy) {
}
