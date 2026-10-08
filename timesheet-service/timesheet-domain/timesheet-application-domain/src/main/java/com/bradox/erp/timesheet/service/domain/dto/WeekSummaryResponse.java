package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WeekSummaryResponse(UUID weekId, UUID employeeId, String employeeName, LocalDate weekStart,
                                  LocalDate weekEnd, WeekStatus status, boolean locked, int totalMinutes,
                                  int expectedMinutes, int overtimeMinutes, Instant submittedAt, String approvedBy,
                                  Instant approvedAt, String refusedReason, boolean canApprove, boolean canReopen,
                                  PostingStatus postingStatus, String postingError, boolean latePosted) {
}
