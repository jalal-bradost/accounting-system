package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(UUID id, UUID projectId, String projectName, String name, String description,
                           TaskStatus status, List<UUID> assigneeEmployeeIds, List<String> assigneeNames,
                           Integer allocatedMinutes, long loggedMinutes, LocalDate deadline, boolean acceptsEntries,
                           UUID saleLineId, String saleLineLabel) {
}
