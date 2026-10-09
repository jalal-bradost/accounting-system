package com.bradox.erp.platform.activity;

/**
 * Maps chatter model names to the module permission required to be an activity assignee.
 * Callers with activity-write can assign only to users holding the matching module permission.
 */
public final class ActivityModelPermissions {

    private ActivityModelPermissions() {}

    /**
     * @return permission code required of the assignee, or {@code null} when any active
     *         company user may be assigned (models without a module-specific gate).
     */
    public static String requiredPermissionForModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return null;
        }
        return switch (modelName.trim()) {
            case "purchase.order", "purchase.vendor.bill" -> "purchase.order.read";
            case "sales.order" -> "sales.order.read";
            case "expense.expense" -> "expense.read";
            case "repair.order" -> "rep.order.view";
            case "project.project", "project.task" -> "project.view";
            // Timesheet: lets the module list the users who may approve any week (tsh.approve_all) as the fallback approvers.
            case "tsh.approval" -> "tsh.approve_all";
            default -> null;
        };
    }
}
