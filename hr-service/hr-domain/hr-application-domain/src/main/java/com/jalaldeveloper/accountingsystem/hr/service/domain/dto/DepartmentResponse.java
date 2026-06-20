package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(UUID id,
                                 UUID companyId,
                                 String name,
                                 UUID parentId,
                                 UUID managerId,
                                 String managerName,
                                 String managerImageUrl,
                                 int colorIndex,
                                 boolean active,
                                 Instant archivedAt,
                                 String archivedBy) {}
