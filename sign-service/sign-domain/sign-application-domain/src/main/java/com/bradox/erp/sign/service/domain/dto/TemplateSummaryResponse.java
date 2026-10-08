package com.bradox.erp.sign.service.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateSummaryResponse(UUID id, String name, String category, int pageCount, boolean active, int roleCount,
                                      Instant createdAt) {
}
