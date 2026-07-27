package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(UUID id,
                                 UUID companyId,
                                 UUID employeeId,
                                 String employeeName,
                                 String employeeImageUrl,
                                 Instant checkIn,
                                 Instant checkOut,
                                 String checkInMode,
                                 String checkOutMode,
                                 int workedMinutes,
                                 int extraHoursMinutes,
                                 String workedHoursLabel,
                                 String displayTitle) {}
