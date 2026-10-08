package com.bradox.erp.timesheet.domain.core.rule;

import java.util.UUID;

/**
 * Who may approve a week (D4, TSH-05 #10): the employee's HR manager, then the department manager, then a
 * Timesheet Manager. Nobody approves their own week unless they are a Timesheet Manager and the company allows it.
 */
public final class ApproverRule {

    private ApproverRule() {
    }

    public static boolean canApprove(UUID actorEmployeeId, UUID ownerEmployeeId, UUID ownerManagerId,
                                     UUID departmentManagerId, boolean hasApprove, boolean hasApproveAll,
                                     boolean managersMaySelfApprove) {
        if (actorEmployeeId != null && actorEmployeeId.equals(ownerEmployeeId)) {
            return hasApproveAll && managersMaySelfApprove;
        }
        if (hasApproveAll) {
            return true;
        }
        return hasApprove && actorEmployeeId != null
                && (actorEmployeeId.equals(ownerManagerId) || actorEmployeeId.equals(departmentManagerId));
    }
}
