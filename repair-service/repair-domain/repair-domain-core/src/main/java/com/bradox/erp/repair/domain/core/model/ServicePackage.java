package com.bradox.erp.repair.domain.core.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServicePackage(UUID id, UUID companyId, String code, String name, String category, boolean active, Instant createdAt,
                             String createdBy, List<PackageLine> lines) {
}
