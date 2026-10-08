package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code warning} is set when the day passes 12 h (BR-TSH-02); the entry is still saved. The cost fields are
 * left out of the JSON unless the caller owns the entry or holds {@code tsh.cost.view} (BR-TSH-10, D15).
 */
public record EntryResponse(UUID id, UUID employeeId, String employeeName, LocalDate workDate, int minutes,
                            UUID projectId, String projectName, String projectCode, UUID taskId, String taskName,
                            String description, boolean billable, EntrySource source, UUID weekId,
                            WeekStatus weekStatus, boolean editable, String warning,
                            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal costRate,
                            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal costAmount,
                            @JsonInclude(JsonInclude.Include.NON_NULL) String costCurrency,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Boolean costMissing, String billingStatus,
                            UUID saleLineId) {
}
