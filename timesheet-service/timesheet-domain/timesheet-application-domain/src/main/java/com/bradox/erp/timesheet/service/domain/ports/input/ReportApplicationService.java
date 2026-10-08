package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.DashboardResponse;
import com.bradox.erp.timesheet.service.domain.dto.ReportResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportApplicationService {

    enum GroupBy { EMPLOYEE, PROJECT, TASK, CUSTOMER, DEPARTMENT, WEEK, MONTH }

    record Query(GroupBy groupBy, LocalDate from, LocalDate to, UUID projectId, Boolean billable,
                 List<WeekStatus> statuses) {
    }

    ReportResponse summary(CompanyId companyId, Query query);

    /** The same rows as {@link #summary} as CSV text. Export is audit-logged (privacy NFR). */
    String exportCsv(CompanyId companyId, Query query);

    DashboardResponse dashboard(CompanyId companyId);
}
