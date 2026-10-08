package com.bradox.erp.timesheet.service.domain.dto;

import java.util.List;
import java.util.UUID;

public record GridRowResponse(UUID projectId, String projectName, String projectCode, UUID taskId, String taskName,
                              boolean projectActive, boolean acceptsEntries, boolean pinned,
                              List<GridCellResponse> cells, int totalMinutes) {
}
