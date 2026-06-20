package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeSummaryResponse(UUID id,
                                      UUID companyId,
                                      String displayName,
                                      String workEmail,
                                      String workPhone,
                                      String jobTitle,
                                      UUID departmentId,
                                      String departmentName,
                                      UUID managerId,
                                      String managerName,
                                      String managerImageUrl,
                                      String imageUrl,
                                      LocalDate hireDate,
                                      boolean active) {}
