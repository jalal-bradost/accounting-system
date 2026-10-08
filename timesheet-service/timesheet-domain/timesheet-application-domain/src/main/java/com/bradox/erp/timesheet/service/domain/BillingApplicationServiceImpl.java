package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;
import com.bradox.erp.timesheet.service.domain.dto.NeedsAttentionItem;
import com.bradox.erp.timesheet.service.domain.dto.RateResponse;
import com.bradox.erp.timesheet.service.domain.dto.SaleLineResponse;
import com.bradox.erp.timesheet.service.domain.dto.SetRatesCommand;
import com.bradox.erp.timesheet.service.domain.ports.input.BillingApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort.SaleLine;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRateRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
class BillingApplicationServiceImpl implements BillingApplicationService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final EntryRepository entries;
    private final WeekRepository weeks;
    private final ProjectRateRepository rates;
    private final EmployeeLookupPort employees;
    private final SalesLineLookupPort lookup;
    private final BillingSync sync;
    private final EntryAssembler assembler;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    BillingApplicationServiceImpl(ProjectRepository projects, TaskRepository tasks, EntryRepository entries,
                                  WeekRepository weeks, ProjectRateRepository rates, EmployeeLookupPort employees,
                                  SalesLineLookupPort lookup, BillingSync sync, EntryAssembler assembler,
                                  AuditLogPort audit, TimesheetAccess access) {
        this.projects = projects;
        this.tasks = tasks;
        this.entries = entries;
        this.weeks = weeks;
        this.rates = rates;
        this.employees = employees;
        this.lookup = lookup;
        this.sync = sync;
        this.assembler = assembler;
        this.audit = audit;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleLineResponse> eligibleLines(CompanyId companyId, UUID customerPartnerId) {
        return lookup.listEligible(companyId, customerPartnerId).stream().map(BillingApplicationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateResponse> rates(CompanyId companyId, UUID projectId) {
        loadProject(companyId, projectId);
        return toRates(companyId, rates.find(new ProjectId(projectId)));
    }

    @Override
    @Transactional
    public List<RateResponse> setRates(CompanyId companyId, UUID projectId, SetRatesCommand command) {
        access.require(TimesheetPermissions.BILLING_MANAGE);
        Project p = loadProject(companyId, projectId);
        if (p.getBillingMode() == BillingMode.FIXED_PRICE) {
            throw new TimesheetDomainException("error.timesheet.fixedPriceNoRates", null,
                    "A fixed-price project does not bill hours, so it has no employee rates");
        }
        Map<UUID, UUID> map = new LinkedHashMap<>();
        for (var r : command.rates()) {
            if (map.put(r.employeeId(), r.saleLineId()) != null) {
                throw new TimesheetDomainException("error.timesheet.duplicateRate", null,
                        "An employee can only have one rate per project");
            }
        }
        if (employees.findAll(companyId, map.keySet()).size() != map.size()) {
            throw new TimesheetDomainException("error.timesheet.employeeNotFound", null, "An employee was not found");
        }
        Map<UUID, SaleLine> lines = lookup.findAll(companyId, Set.copyOf(map.values()));
        for (UUID line : map.values()) {
            requireEligible(lines.get(line));
        }
        rates.replace(companyId, p.getId(), map);
        audit.recordBusinessEvent(companyId, "tsh.project", projectId, "Project rates updated", Map.of("rates", map.size()));
        return toRates(companyId, map);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NeedsAttentionItem> needsAttention(CompanyId companyId) {
        access.require(TimesheetPermissions.BILLING_MANAGE);
        List<Entry> noLine = entries.findApprovedBillableWithoutLine(companyId);
        List<NeedsAttentionItem> out = new java.util.ArrayList<>();
        Map<UUID, EmployeeRef> people = employees.findAll(companyId, noLine.stream().map(Entry::getEmployeeId).collect(Collectors.toSet()));
        Map<ProjectId, Project> projectById = projects.findByIds(noLine.stream().map(Entry::getProjectId).distinct().toList())
                .stream().collect(Collectors.toMap(Project::getId, p -> p));
        Map<TaskId, Task> taskById = tasks.findByIds(noLine.stream().map(Entry::getTaskId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Task::getId, t -> t));
        for (Entry e : noLine) {
            out.add(item(e, "NO_LINE", people, projectById, taskById, null));
        }
        // Mapped entries whose order was closed after approval also need a person to decide (TSH-06 #8).
        List<Entry> mapped = entries.findApprovedBillableByLines(companyId, closedCandidateLines(companyId));
        if (!mapped.isEmpty()) {
            Map<UUID, SaleLine> infos = lookup.findAll(companyId, mapped.stream().map(Entry::getSaleLineId).collect(Collectors.toSet()));
            Map<UUID, EmployeeRef> more = employees.findAll(companyId, mapped.stream().map(Entry::getEmployeeId).collect(Collectors.toSet()));
            Map<ProjectId, Project> morePro = projects.findByIds(mapped.stream().map(Entry::getProjectId).distinct().toList())
                    .stream().collect(Collectors.toMap(Project::getId, p -> p));
            Map<TaskId, Task> moreTasks = tasks.findByIds(mapped.stream().map(Entry::getTaskId).filter(Objects::nonNull).distinct().toList())
                    .stream().collect(Collectors.toMap(Task::getId, t -> t));
            for (Entry e : mapped) {
                SaleLine info = infos.get(e.getSaleLineId());
                if (info != null && info.closed()) {
                    out.add(item(e, "ORDER_CLOSED", more, morePro, moreTasks, info));
                }
            }
        }
        out.sort(Comparator.comparing(NeedsAttentionItem::workDate));
        return out;
    }

    /** The mapped lines to inspect for closed orders: every line used by approved billable entries. */
    private Set<UUID> closedCandidateLines(CompanyId companyId) {
        // Lines used by project defaults, task lines and rates cover everything a new entry could map to.
        Set<UUID> lines = new java.util.LinkedHashSet<>();
        projects.findAll(companyId, true).forEach(p -> {
            if (p.getDefaultSaleLineId() != null) lines.add(p.getDefaultSaleLineId());
            lines.addAll(rates.find(p.getId()).values());
            tasks.findByProject(companyId, p.getId()).forEach(t -> {
                if (t.getSaleLineId() != null) lines.add(t.getSaleLineId());
            });
        });
        return lines;
    }

    @Override
    @Transactional
    public EntryResponse assignLine(CompanyId companyId, UUID entryId, UUID saleLineId) {
        access.require(TimesheetPermissions.BILLING_MANAGE);
        Entry e = entries.find(new EntryId(entryId)).filter(x -> x.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));
        if (!e.isBillable()) {
            throw new TimesheetDomainException("error.timesheet.entryNotBillable", null, "This entry is not billable");
        }
        SaleLine target = lookup.find(companyId, saleLineId).orElse(null);
        requireEligible(target);
        if (target.closed()) {
            throw new TimesheetDomainException("error.timesheet.orderClosed", null, "That order is closed");
        }
        UUID previous = e.getSaleLineId();
        e.assignSaleLine(saleLineId);
        entries.save(e);
        TimesheetWeek week = weeks.findByIds(List.of(e.getWeekId())).stream().findFirst().orElse(null);
        if (week != null && week.getStatus() == WeekStatus.APPROVED) {
            Set<UUID> touched = new java.util.LinkedHashSet<>();
            touched.add(saleLineId);
            if (previous != null) touched.add(previous);
            sync.recompute(companyId, touched, "line assigned by " + access.actorLabel());
        }
        audit.recordBusinessEvent(companyId, "tsh.entry", entryId, "Sales line assigned",
                Map.of("saleLineId", saleLineId.toString()));
        return assembler.toResponse(companyId, e, null);
    }

    private void requireEligible(SaleLine line) {
        if (line == null) {
            throw new TimesheetDomainException("error.timesheet.saleLineNotFound", null, "Sales order line not found");
        }
        if (!line.eligible()) {
            throw new TimesheetDomainException("error.timesheet.saleLineNotEligible", null,
                    "That line is not a service line invoiced from timesheets");
        }
    }

    private Project loadProject(CompanyId companyId, UUID id) {
        return projects.find(new ProjectId(id)).filter(p -> p.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private List<RateResponse> toRates(CompanyId companyId, Map<UUID, UUID> map) {
        Map<UUID, EmployeeRef> people = employees.findAll(companyId, map.keySet());
        Map<UUID, SaleLine> lines = lookup.findAll(companyId, Set.copyOf(map.values()));
        return map.entrySet().stream().map(en -> new RateResponse(en.getKey(),
                people.containsKey(en.getKey()) ? people.get(en.getKey()).name() : null, en.getValue(),
                lines.containsKey(en.getValue()) ? lines.get(en.getValue()).label() : null))
                .sorted(Comparator.comparing(r -> r.employeeName() == null ? "" : r.employeeName().toLowerCase())).toList();
    }

    private static NeedsAttentionItem item(Entry e, String reason, Map<UUID, EmployeeRef> people, Map<ProjectId, Project> projects,
                                           Map<TaskId, Task> tasks, SaleLine line) {
        EmployeeRef emp = people.get(e.getEmployeeId());
        Project p = projects.get(e.getProjectId());
        Task t = e.getTaskId() == null ? null : tasks.get(e.getTaskId());
        return new NeedsAttentionItem(e.getId().getId(), e.getEmployeeId(), emp == null ? null : emp.name(), e.getWorkDate(),
                e.getMinutes(), e.getProjectId().getId(), p == null ? null : p.getName(), t == null ? null : t.getName(),
                reason, e.getSaleLineId(), line == null ? null : line.label());
    }

    static SaleLineResponse toResponse(SaleLine l) {
        return new SaleLineResponse(l.lineId(), l.orderId(), l.orderName(), l.customerPartnerId(), l.lineName(), l.label(),
                l.unit(), l.eligible(), l.closed(), l.qtyDelivered(), l.qtyInvoiced());
    }
}
