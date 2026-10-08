package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

/** A billable entry that cannot change a quantity yet. {@code reason} is NO_LINE or ORDER_CLOSED (TSH-06 #6, #8). */
public record NeedsAttentionItem(UUID entryId, UUID employeeId, String employeeName, LocalDate workDate, int minutes,
                                 UUID projectId, String projectName, String taskName, String reason, UUID saleLineId,
                                 String saleLineLabel) {
}
