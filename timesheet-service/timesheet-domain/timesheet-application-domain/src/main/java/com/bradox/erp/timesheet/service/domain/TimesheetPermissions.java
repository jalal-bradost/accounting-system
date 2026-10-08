package com.bradox.erp.timesheet.service.domain;

/** Permission codes from EPIC-TSH. */
public final class TimesheetPermissions {

    public static final String ENTRY_OWN = "tsh.entry.own";
    public static final String ENTRY_VIEW_TEAM = "tsh.entry.view_team";
    public static final String ENTRY_VIEW_ALL = "tsh.entry.view_all";
    public static final String ENTRY_ON_BEHALF = "tsh.entry.on_behalf";
    public static final String APPROVE = "tsh.approve";
    public static final String APPROVE_ALL = "tsh.approve_all";
    public static final String REOPEN = "tsh.reopen";
    public static final String PROJECT_MANAGE = "tsh.project.manage";
    public static final String TASK_MANAGE = "tsh.task.manage";
    public static final String COST_VIEW = "tsh.cost.view";
    public static final String REPORT_VIEW = "tsh.report.view";
    public static final String BILLING_MANAGE = "tsh.billing.manage";
    public static final String POSTING_MANAGE = "tsh.posting.manage";
    public static final String SETTINGS_MANAGE = "tsh.settings.manage";

    private TimesheetPermissions() {
    }
}
