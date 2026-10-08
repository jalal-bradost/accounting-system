package com.bradox.erp.timesheet.service.domain.dto;

import java.util.List;

public record EntryListResponse(List<EntryResponse> items, long totalMinutes, long billableMinutes) {
}
