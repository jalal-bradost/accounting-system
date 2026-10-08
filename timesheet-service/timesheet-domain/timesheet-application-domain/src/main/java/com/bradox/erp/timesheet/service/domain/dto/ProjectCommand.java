package com.bradox.erp.timesheet.service.domain.dto;

import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * {@code allocatedMinutes} is whole minutes; the UI converts from hours. {@code defaultSaleLineId} needs
 * {@code tsh.billing.manage}; {@code costAccountId} needs {@code tsh.posting.manage}.
 */
public record ProjectCommand(@NotBlank String name, String code, UUID partnerId, UUID managerEmployeeId,
                             Boolean allowTimesheets, BillingMode billingMode, Boolean billableDefault,
                             Integer allocatedMinutes, String color, UUID defaultSaleLineId, UUID costAccountId) {

    public ProjectCommand(String name, String code, UUID partnerId, UUID managerEmployeeId, Boolean allowTimesheets,
                          BillingMode billingMode, Boolean billableDefault, Integer allocatedMinutes, String color) {
        this(name, code, partnerId, managerEmployeeId, allowTimesheets, billingMode, billableDefault, allocatedMinutes,
                color, null, null);
    }
}
