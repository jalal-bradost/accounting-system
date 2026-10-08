package com.bradox.erp.timesheet.service.domain.dto;

import java.util.List;
import java.util.UUID;

public record BulkApproveResponse(List<Item> items, int approved, int failed) {

    public record Item(UUID weekId, boolean ok, String message) {
    }
}
