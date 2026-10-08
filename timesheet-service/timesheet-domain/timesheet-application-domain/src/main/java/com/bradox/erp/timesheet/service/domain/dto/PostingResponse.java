package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PostingResponse(UUID id, UUID weekId, UUID employeeId, String employeeName, LocalDate weekStart, int version,
                              PostingStatus status, UUID journalEntryId, UUID reversalEntryId, LocalDate entryDate,
                              boolean latePosted, BigDecimal totalAmount, int attempts, String errorMessage,
                              Instant postedAt, List<Line> lines) {

    public record Line(UUID projectId, String projectName, UUID debitAccountId, BigDecimal amount, int minutes) {
    }
}
