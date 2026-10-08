package com.bradox.erp.timesheet.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportResponse(String groupBy, String scope, List<ReportRowResponse> rows, long totalMinutes,
                             long billableMinutes, BigDecimal costAmount) {
}
