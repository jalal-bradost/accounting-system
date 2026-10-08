package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Report sums done by the database (TSH-08 #6). The grouping expression comes from a fixed switch, never from
 * request input; every filter value is a bound parameter.
 */
@Component
public class ReportRepositoryImpl implements ReportRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Row> aggregate(CompanyId companyId, Filter f, Dimension dimension) {
        if (f.employeeIds() != null && f.employeeIds().isEmpty()) {
            return List.of();
        }
        String key = switch (dimension) {
            case EMPLOYEE -> "cast(e.employee_id as varchar(36))";
            case PROJECT -> "cast(e.project_id as varchar(36))";
            case TASK -> "cast(e.task_id as varchar(36))";
            case WEEK -> "cast(w.week_start as varchar(10))";
            case MONTH -> "substring(cast(e.work_date as varchar(10)), 1, 7)";
        };
        StringBuilder sql = new StringBuilder("select ").append(key).append(" as k, sum(e.minutes), "
                + "sum(case when e.billable = true then e.minutes else 0 end), coalesce(sum(e.cost_amount), 0), count(*) "
                + "from tsh_entry e join tsh_week w on w.id = e.week_id where e.company_id = :company");
        if (f.employeeIds() != null) sql.append(" and e.employee_id in (:employees)");
        if (f.from() != null) sql.append(" and e.work_date >= :from");
        if (f.to() != null) sql.append(" and e.work_date <= :to");
        if (f.projectId() != null) sql.append(" and e.project_id = :project");
        if (f.billable() != null) sql.append(" and e.billable = :billable");
        if (f.statuses() != null && !f.statuses().isEmpty()) sql.append(" and w.status in (:statuses)");
        sql.append(" group by ").append(key).append(" order by sum(e.minutes) desc");

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("company", companyId.getId());
        if (f.employeeIds() != null) q.setParameter("employees", f.employeeIds());
        if (f.from() != null) q.setParameter("from", Date.valueOf(f.from()));
        if (f.to() != null) q.setParameter("to", Date.valueOf(f.to()));
        if (f.projectId() != null) q.setParameter("project", f.projectId());
        if (f.billable() != null) q.setParameter("billable", f.billable());
        if (f.statuses() != null && !f.statuses().isEmpty()) {
            q.setParameter("statuses", f.statuses().stream().map(Enum::name).toList());
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<Row> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            out.add(new Row(r[0] == null ? null : r[0].toString(), ((Number) r[1]).longValue(), ((Number) r[2]).longValue(),
                    r[3] instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) r[3]).doubleValue()),
                    ((Number) r[4]).longValue()));
        }
        return out;
    }
}
