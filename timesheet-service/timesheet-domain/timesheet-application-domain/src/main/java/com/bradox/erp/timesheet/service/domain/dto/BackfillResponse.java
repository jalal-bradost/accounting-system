package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BackfillResponse(List<Item> items, int posted, int failed, int skipped) {

    public record Item(UUID weekId, String employeeName, LocalDate weekStart, PostingStatus status, String message) {
    }
}
