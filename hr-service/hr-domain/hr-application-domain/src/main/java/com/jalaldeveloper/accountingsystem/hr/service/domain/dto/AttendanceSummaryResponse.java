package com.jalaldeveloper.accountingsystem.hr.service.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendanceSummaryResponse(UUID id,
                                        UUID employeeId,
                                        String employeeName,
                                        String employeeImageUrl,
                                        Instant checkIn,
                                        Instant checkOut,
                                        int workedMinutes,
                                        int extraHoursMinutes,
                                        String workedHoursLabel,
                                        String displayTitle) {}
