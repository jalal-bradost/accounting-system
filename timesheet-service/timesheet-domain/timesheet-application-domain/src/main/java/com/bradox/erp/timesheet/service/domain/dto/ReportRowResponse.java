package com.bradox.erp.timesheet.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** {@code costAmount} is left out of the JSON entirely without {@code tsh.cost.view} (BR-TSH-10). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportRowResponse(String key, String label, long minutes, long billableMinutes, long entryCount,
                                BigDecimal costAmount) {
}
