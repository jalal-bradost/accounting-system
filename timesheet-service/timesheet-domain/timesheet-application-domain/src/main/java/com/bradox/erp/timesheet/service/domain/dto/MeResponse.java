package com.bradox.erp.timesheet.service.domain.dto;

import java.time.LocalDate;

/** What the UI needs to open "My Timesheets": who am I, how to show time, what is today. */
public record MeResponse(EmployeeRefResponse employee, SettingsResponse settings, LocalDate today,
                         boolean canLogOnBehalf, boolean canViewAll, boolean canManageProjects,
                         boolean canApprove, boolean canViewTeam, boolean canViewCost, boolean canViewReports,
                         boolean canManageSettings, boolean canManageBilling, boolean canManagePostings) {
}
