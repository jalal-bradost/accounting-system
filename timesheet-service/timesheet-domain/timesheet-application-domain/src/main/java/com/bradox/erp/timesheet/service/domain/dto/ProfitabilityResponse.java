package com.bradox.erp.timesheet.service.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Cost side of project profitability (TSH-07 #5). Revenue and margin arrive with billing (TSH-06). */
public record ProfitabilityResponse(UUID projectId, long loggedMinutes, long billableMinutes, Integer allocatedMinutes,
                                    BigDecimal laborCost, long missingCostEntries) {
}
