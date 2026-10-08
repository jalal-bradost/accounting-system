package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Database-side aggregation for reports (TSH-08 #6): no entity loading. */
public interface ReportRepository {

    enum Dimension { EMPLOYEE, PROJECT, TASK, WEEK, MONTH }

    record Filter(Collection<UUID> employeeIds, LocalDate from, LocalDate to, UUID projectId, Boolean billable,
                  Collection<WeekStatus> statuses) {
    }

    /** {@code key} is the id or bucket as text; null for entries without a task. */
    record Row(String key, long minutes, long billableMinutes, BigDecimal costAmount, long entryCount) {
    }

    /** {@code filter.employeeIds()} null means everyone; an empty collection yields no rows. */
    List<Row> aggregate(CompanyId companyId, Filter filter, Dimension dimension);
}
