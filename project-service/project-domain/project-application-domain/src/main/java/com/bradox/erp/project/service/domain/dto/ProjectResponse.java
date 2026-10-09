package com.bradox.erp.project.service.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, UUID customerPartnerId, String managerUsername, LocalDate startDate,
                              LocalDate endDate, int color, long taskCount, Instant createdAt) {
}
