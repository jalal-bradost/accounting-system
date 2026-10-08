package com.bradox.erp.timesheet.service.domain.dto;

import java.util.List;

/** TSH-08 #3. {@code scope} is OWN, TEAM or ALL, from the caller's role (TSH-08 #4). */
public record DashboardResponse(String scope, long minutesThisWeek, long minutesThisMonth, int billablePercent,
                                int utilizationPercent, long expectedMinutesThisWeek, long overtimeMinutes,
                                List<EmployeeRefResponse> missingLastWeek, List<ReportRowResponse> topProjects,
                                LedgerVariance ledger) {

    /**
     * TSH-10 #9, only for people who may see cost. Payroll books the real salary; timesheets reclassify standard cost
     * to jobs, so what is left in "Labor cost applied" is the variance: salary expense minus labor cost applied.
     */
    public record LedgerVariance(java.math.BigDecimal laborApplied, java.math.BigDecimal salaryExpense,
                                 java.math.BigDecimal variance) {
    }
}
