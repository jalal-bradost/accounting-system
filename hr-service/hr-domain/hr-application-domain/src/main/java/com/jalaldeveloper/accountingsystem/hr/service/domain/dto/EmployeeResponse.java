package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(UUID id,
                               UUID companyId,
                               String displayName,
                               String workEmail,
                               String workPhone,
                               String mobilePhone,
                               String jobTitle,
                               UUID departmentId,
                               String departmentName,
                               UUID managerId,
                               String managerName,
                               String managerImageUrl,
                               UUID userId,
                               String userDisplayName,
                               String userEmail,
                               LocalDate hireDate,
                               String workStreet,
                               String workCity,
                               String workState,
                               String workPostalCode,
                               String workCountry,
                               String workLocation,
                               String imageUrl,
                               String imageContentType,
                               boolean active,
                               Instant archivedAt,
                               String archivedBy) {}
