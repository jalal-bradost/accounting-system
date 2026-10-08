package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.rule.WeekCalendar;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.DashboardResponse;
import com.bradox.erp.timesheet.service.domain.dto.EmployeeRefResponse;
import com.bradox.erp.timesheet.service.domain.dto.ReportResponse;
import com.bradox.erp.timesheet.service.domain.dto.ReportRowResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.ReportApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.PartnerLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReportRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ReportRepository.Dimension;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Reports and the dashboard (TSH-08). Sums run in the database; scope comes from the caller's role. */
@Service
class ReportApplicationServiceImpl implements ReportApplicationService {

    private static final int EXPECTED_EMPLOYEE_CAP = 200;

    private final ReportRepository reports;
    private final EmployeeLookupPort employees;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final PartnerLookupPort partners;
    private final WeekRepository weeks;
    private final ExpectedHoursPort expectedHours;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final PostingRepository postings;
    private final LaborCostPostingPort ledger;

    ReportApplicationServiceImpl(ReportRepository reports, EmployeeLookupPort employees, ProjectRepository projects,
                                 TaskRepository tasks, PartnerLookupPort partners, WeekRepository weeks,
                                 ExpectedHoursPort expectedHours, AuditLogPort audit, TimesheetAccess access,
                                 PostingRepository postings, LaborCostPostingPort ledger) {
        this.reports = reports;
        this.employees = employees;
        this.projects = projects;
        this.tasks = tasks;
        this.partners = partners;
        this.weeks = weeks;
        this.expectedHours = expectedHours;
        this.audit = audit;
        this.access = access;
        this.postings = postings;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse summary(CompanyId companyId, Query q) {
        TimesheetAccess.Scope scope = access.reportScope(companyId);
        Set<UUID> ids = scopeIds(companyId, scope);
        boolean showCost = access.can(TimesheetPermissions.COST_VIEW) || scope == TimesheetAccess.Scope.OWN;
        List<ReportRowResponse> rows = rows(companyId, q, ids, showCost);
        long total = rows.stream().mapToLong(ReportRowResponse::minutes).sum();
        long billable = rows.stream().mapToLong(ReportRowResponse::billableMinutes).sum();
        BigDecimal cost = showCost ? rows.stream().map(r -> r.costAmount() == null ? BigDecimal.ZERO : r.costAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add) : null;
        return new ReportResponse(q.groupBy().name(), scope.name(), rows, total, billable, cost);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv(CompanyId companyId, Query q) {
        ReportResponse r = summary(companyId, q);
        boolean cost = r.costAmount() != null;
        StringBuilder sb = new StringBuilder("﻿").append(q.groupBy().name().toLowerCase())
                .append(",hours,billable_hours,entries").append(cost ? ",cost" : "").append('\n');
        for (ReportRowResponse row : r.rows()) {
            sb.append(csv(row.label())).append(',').append(hours(row.minutes())).append(',')
                    .append(hours(row.billableMinutes())).append(',').append(row.entryCount());
            if (cost) {
                sb.append(',').append(row.costAmount() == null ? "" : row.costAmount().toPlainString());
            }
            sb.append('\n');
        }
        audit.recordBusinessEvent(companyId, "tsh.report", companyId.getId(), "Report exported",
                Map.of("groupBy", q.groupBy().name(), "rows", r.rows().size(), "scope", r.scope(), "by", access.actorLabel()));
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(CompanyId companyId) {
        TimesheetAccess.Scope scope = access.reportScope(companyId);
        Set<UUID> ids = scopeIds(companyId, scope);
        TimesheetSettings settings = access.settings(companyId);
        LocalDate today = access.today(settings);
        LocalDate weekStart = WeekCalendar.weekStart(today, settings.getWeekStartDay());
        LocalDate monthStart = today.withDayOfMonth(1);
        if (ids != null && ids.isEmpty()) {
            return new DashboardResponse(scope.name(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), null);
        }
        var weekRows = reports.aggregate(companyId, new ReportRepository.Filter(ids, weekStart, weekStart.plusDays(6), null, null, null),
                Dimension.EMPLOYEE);
        var monthRows = reports.aggregate(companyId, new ReportRepository.Filter(ids, monthStart, today, null, null, null),
                Dimension.EMPLOYEE);
        long week = weekRows.stream().mapToLong(ReportRepository.Row::minutes).sum();
        long month = monthRows.stream().mapToLong(ReportRepository.Row::minutes).sum();
        long monthBillable = monthRows.stream().mapToLong(ReportRepository.Row::billableMinutes).sum();

        List<EmployeeRef> people = people(companyId, scope, ids);
        long expected = 0;
        long overtime = 0;
        Map<String, Long> loggedByEmployee = weekRows.stream().collect(Collectors.toMap(ReportRepository.Row::key,
                ReportRepository.Row::minutes, Long::sum));
        for (EmployeeRef p : people.stream().limit(EXPECTED_EMPLOYEE_CAP).toList()) {
            long exp = expectedHours.expected(companyId, p.id(), weekStart, weekStart.plusDays(6)).values().stream()
                    .mapToLong(ExpectedHoursPort.ExpectedDay::minutes).sum();
            expected += exp;
            // Without a schedule nothing is expected, so nothing can be overtime.
            if (exp > 0) {
                overtime += Math.max(0, loggedByEmployee.getOrDefault(p.id().toString(), 0L) - exp);
            }
        }

        LocalDate lastWeek = weekStart.minusDays(7);
        Set<UUID> done = weeks.findForEmployees(companyId, lastWeek, people.stream().map(EmployeeRef::id).toList()).stream()
                .filter(w -> w.getStatus() == WeekStatus.SUBMITTED || w.getStatus() == WeekStatus.APPROVED)
                .map(w -> w.getEmployeeId()).collect(Collectors.toSet());
        List<EmployeeRefResponse> missing = people.stream().filter(p -> !done.contains(p.id()))
                .map(p -> new EmployeeRefResponse(p.id(), p.name())).limit(50).toList();

        var projectRows = rows(companyId, new Query(GroupBy.PROJECT, monthStart, today, null, null, null), ids, false)
                .stream().limit(5).toList();
        return new DashboardResponse(scope.name(), week, month, month == 0 ? 0 : (int) Math.round(monthBillable * 100.0 / month),
                expected == 0 ? 0 : (int) Math.round(week * 100.0 / expected), expected, overtime, missing, projectRows,
                ledgerVariance(companyId, settings, monthStart, today));
    }

    /** Only for cost viewers, and only when posting is on and there is something posted this month. */
    private DashboardResponse.LedgerVariance ledgerVariance(CompanyId companyId, TimesheetSettings settings, LocalDate from,
                                                             LocalDate to) {
        if (!settings.isLedgerPostingEnabled()
                || !(access.can(TimesheetPermissions.POSTING_MANAGE) || access.can(TimesheetPermissions.COST_VIEW))) {
            return null;
        }
        try {
            BigDecimal applied = postings.sumPosted(companyId, from, to);
            BigDecimal salary = ledger.salaryExpense(companyId, from, to);
            return new DashboardResponse.LedgerVariance(applied, salary, salary.subtract(applied));
        } catch (RuntimeException ex) {
            return null;   // the dashboard still works if the ledger cannot be read
        }
    }

    /** null means everyone. */
    private Set<UUID> scopeIds(CompanyId companyId, TimesheetAccess.Scope scope) {
        UUID me = access.actor(companyId).map(EmployeeRef::id).orElse(null);
        return switch (scope) {
            case ALL -> null;
            case TEAM -> {
                Set<UUID> s = new HashSet<>(access.teamIds(companyId));
                if (me != null) s.add(me);
                yield s;
            }
            case OWN -> me == null ? Set.of() : Set.of(me);
        };
    }

    private List<EmployeeRef> people(CompanyId companyId, TimesheetAccess.Scope scope, Set<UUID> ids) {
        if (ids == null) {
            return employees.listActive(companyId);
        }
        return employees.findAll(companyId, ids).values().stream().toList();
    }

    private List<ReportRowResponse> rows(CompanyId companyId, Query q, Set<UUID> ids, boolean showCost) {
        if (ids != null && ids.isEmpty()) {
            return List.of();
        }
        Dimension dim = switch (q.groupBy()) {
            case CUSTOMER -> Dimension.PROJECT;
            case DEPARTMENT -> Dimension.EMPLOYEE;
            case EMPLOYEE -> Dimension.EMPLOYEE;
            case PROJECT -> Dimension.PROJECT;
            case TASK -> Dimension.TASK;
            case WEEK -> Dimension.WEEK;
            case MONTH -> Dimension.MONTH;
        };
        var raw = reports.aggregate(companyId, new ReportRepository.Filter(ids, q.from(), q.to(), q.projectId(), q.billable(),
                q.statuses()), dim);
        Map<String, String> labels = labels(companyId, q.groupBy(), raw);
        Map<String, long[]> acc = new LinkedHashMap<>();
        Map<String, BigDecimal> cost = new LinkedHashMap<>();
        for (var r : raw) {
            String bucket = switch (q.groupBy()) {
                case CUSTOMER, DEPARTMENT -> labels.getOrDefault(r.key(), "—");
                default -> r.key() == null ? "—" : r.key();
            };
            long[] a = acc.computeIfAbsent(bucket, k -> new long[3]);
            a[0] += r.minutes();
            a[1] += r.billableMinutes();
            a[2] += r.entryCount();
            cost.merge(bucket, r.costAmount() == null ? BigDecimal.ZERO : r.costAmount(), BigDecimal::add);
        }
        boolean grouped = q.groupBy() == GroupBy.CUSTOMER || q.groupBy() == GroupBy.DEPARTMENT;
        List<ReportRowResponse> out = new ArrayList<>();
        acc.forEach((k, a) -> out.add(new ReportRowResponse(k, grouped ? k : labels.getOrDefault(k, k), a[0], a[1], a[2],
                showCost ? cost.get(k) : null)));
        boolean chronological = q.groupBy() == GroupBy.WEEK || q.groupBy() == GroupBy.MONTH;
        out.sort(chronological ? Comparator.comparing(ReportRowResponse::key)
                : Comparator.comparingLong(ReportRowResponse::minutes).reversed());
        return out;
    }

    private Map<String, String> labels(CompanyId companyId, GroupBy by, List<ReportRepository.Row> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (by == GroupBy.WEEK || by == GroupBy.MONTH) {
            return out;   // the key is the bucket itself ("2026-06-20", "2026-06"), not an id
        }
        List<UUID> keys = raw.stream().map(ReportRepository.Row::key).filter(Objects::nonNull).map(UUID::fromString).toList();
        switch (by) {
            case EMPLOYEE -> employees.findAll(companyId, keys).forEach((id, e) -> out.put(id.toString(), e.name()));
            case PROJECT -> projects.findByIds(keys.stream().map(ProjectId::new).toList())
                    .forEach(p -> out.put(p.getId().getId().toString(), p.getName()));
            case TASK -> tasks.findByIds(keys.stream().map(TaskId::new).toList())
                    .forEach(t -> out.put(t.getId().getId().toString(), t.getName()));
            case CUSTOMER -> {
                List<Project> ps = projects.findByIds(keys.stream().map(ProjectId::new).toList());
                Map<UUID, String> names = partners.names(companyId,
                        ps.stream().map(Project::getPartnerId).filter(Objects::nonNull).collect(Collectors.toSet()));
                ps.forEach(p -> out.put(p.getId().getId().toString(),
                        p.getPartnerId() == null ? "—" : names.getOrDefault(p.getPartnerId(), "—")));
            }
            case DEPARTMENT -> {
                Map<UUID, EmployeeRef> people = employees.findAll(companyId, keys);
                Map<UUID, String> names = employees.departmentNames(companyId,
                        people.values().stream().map(EmployeeRef::departmentId).filter(Objects::nonNull).collect(Collectors.toSet()));
                people.forEach((id, e) -> out.put(id.toString(),
                        e.departmentId() == null ? "—" : names.getOrDefault(e.departmentId(), "—")));
            }
            default -> {
            }
        }
        return out;
    }

    private static String hours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        String safe = s.replace("\"", "\"\"");
        // Guard against spreadsheet formula injection from user-entered names.
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return safe.contains(",") || safe.contains("\"") || safe.contains("\n") ? "\"" + safe + "\"" : safe;
    }
}
