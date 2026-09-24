package com.bradox.erp.hr.service.domain.dto;

import java.util.UUID;

public record DepartmentSummaryResponse(UUID id,
                                        String name,
                                        UUID parentId,
                                        UUID managerId,
                                        String managerName,
                                        String managerImageUrl,
                                        int colorIndex,
                                        long employeeCount,
                                        boolean active) {}
