package com.bradox.erp.timesheet.domain.core.rule;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * BR-TSH-12: which sales order line an entry bills to. In order: the task's line, then the project's employee-rate
 * line for that employee, then the project's default line. Empty means "billable but no order line".
 */
public final class BillingResolver {

    private BillingResolver() {
    }

    public static Optional<UUID> resolve(UUID taskSaleLineId, Map<UUID, UUID> employeeRateLines, UUID employeeId,
                                         UUID projectDefaultLineId) {
        if (taskSaleLineId != null) {
            return Optional.of(taskSaleLineId);
        }
        UUID rate = employeeRateLines == null ? null : employeeRateLines.get(employeeId);
        if (rate != null) {
            return Optional.of(rate);
        }
        return Optional.ofNullable(projectDefaultLineId);
    }
}
