package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;

import java.util.UUID;

public record ProjectResponse(UUID id, String name, String code, UUID partnerId, String partnerName,
                              UUID managerEmployeeId, String managerName, boolean allowTimesheets,
                              BillingMode billingMode, boolean billableDefault, Integer allocatedMinutes,
                              long loggedMinutes, ProjectStatus status, String color, boolean systemManaged,
                              boolean internal, UUID defaultSaleLineId, String defaultSaleLineLabel, UUID costAccountId) {
}
