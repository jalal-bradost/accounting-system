package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.exception.DomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.Task;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.BillingQuantity;
import com.bradox.erp.timesheet.domain.core.rule.BillingResolver;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineBillingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.SalesLineLookupPort.SaleLine;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRateRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Keeps the delivered quantity of timesheet-billed sales lines equal to the approved billable hours (TSH-06, BR-TSH-12
 * to 14). The quantity is always recomputed from the approved entries, never incremented by hand.
 */
@Component
class BillingSync {

    static final String AUDIT_MODEL = "tsh.billing";

    private final EntryRepository entries;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final ProjectRateRepository rates;
    private final SalesLineLookupPort lookup;
    private final SalesLineBillingPort billing;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    BillingSync(EntryRepository entries, ProjectRepository projects, TaskRepository tasks, ProjectRateRepository rates,
                SalesLineLookupPort lookup, SalesLineBillingPort billing, AuditLogPort audit, TimesheetAccess access) {
        this.entries = entries;
        this.projects = projects;
        this.tasks = tasks;
        this.rates = rates;
        this.lookup = lookup;
        this.billing = billing;
        this.audit = audit;
        this.access = access;
    }

    /** Resolves each billable entry's order line (BR-TSH-12), then recomputes the lines it touches. */
    void onApproved(CompanyId companyId, TimesheetWeek week, List<Entry> weekEntries) {
        Map<ProjectId, Project> projectById = projects.findByIds(
                weekEntries.stream().map(Entry::getProjectId).distinct().toList()).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));
        Map<UUID, Map<UUID, UUID>> rateByProject = rates.findForProjects(projectById.keySet()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<TaskId, Task> taskById = tasks.findByIds(weekEntries.stream().map(Entry::getTaskId).filter(Objects::nonNull)
                .distinct().toList()).stream().collect(Collectors.toMap(Task::getId, t -> t));
        Set<UUID> lines = new LinkedHashSet<>();
        for (Entry e : weekEntries) {
            Project p = projectById.get(e.getProjectId());
            if (!e.isBillable() || p == null || p.getBillingMode() != BillingMode.HOURLY) {
                continue;
            }
            Task t = e.getTaskId() == null ? null : taskById.get(e.getTaskId());
            UUID line = BillingResolver.resolve(t == null ? null : t.getSaleLineId(),
                    rateByProject.getOrDefault(p.getId().getId(), Map.of()), e.getEmployeeId(), p.getDefaultSaleLineId())
                    .orElse(null);
            e.assignSaleLine(line);
            entries.save(e);
            if (line != null) {
                lines.add(line);
            }
        }
        recompute(companyId, lines, "approved week " + week.getWeekStart());
    }

    /**
     * BR-TSH-14: an approved week whose hours are already invoiced cannot be reopened if the line would fall below
     * the invoiced quantity. Call before the week changes state.
     */
    void checkReopenAllowed(CompanyId companyId, List<Entry> weekEntries) {
        Map<UUID, Long> weekMinutesByLine = weekEntries.stream().filter(e -> e.isBillable() && e.getSaleLineId() != null)
                .collect(Collectors.groupingBy(Entry::getSaleLineId, Collectors.summingLong(Entry::getMinutes)));
        if (weekMinutesByLine.isEmpty()) {
            return;
        }
        Map<UUID, Long> now = entries.sumApprovedBillableMinutesByLine(companyId, weekMinutesByLine.keySet());
        Map<UUID, SaleLine> infos = lookup.findAll(companyId, weekMinutesByLine.keySet());
        int perDay = access.settings(companyId).getMinutesPerDay();
        for (var e : weekMinutesByLine.entrySet()) {
            SaleLine info = infos.get(e.getKey());
            if (info == null || !info.eligible()) {
                continue;
            }
            long remaining = now.getOrDefault(e.getKey(), 0L) - e.getValue();
            BigDecimal qty = BillingQuantity.toQuantity(Math.max(0, remaining), info.unit(), perDay);
            if (qty.compareTo(info.qtyInvoiced()) < 0) {
                throw new TimesheetDomainException("error.timesheet.reopenBlockedInvoiced",
                        new Object[]{info.label(), info.qtyInvoiced()},
                        "This week cannot be reopened: " + info.label() + " has already invoiced "
                                + info.qtyInvoiced().stripTrailingZeros().toPlainString()
                                + ", more than the hours that would remain. Issue a credit note first.");
            }
        }
    }

    /** After the reopen: forget the mappings and recompute the lines without this week's hours. */
    void onReopened(CompanyId companyId, List<Entry> weekEntries) {
        Set<UUID> lines = new LinkedHashSet<>();
        for (Entry e : weekEntries) {
            if (e.getSaleLineId() != null) {
                lines.add(e.getSaleLineId());
                e.assignSaleLine(null);
                entries.save(e);
            }
        }
        recompute(companyId, lines, "reopened week");
    }

    /** Sets each line's delivered quantity to the approved billable hours. Returns the lines that were updated. */
    Set<UUID> recompute(CompanyId companyId, Collection<UUID> lineIds, String source) {
        Set<UUID> updated = new LinkedHashSet<>();
        if (lineIds.isEmpty()) {
            return updated;
        }
        Map<UUID, Long> minutes = entries.sumApprovedBillableMinutesByLine(companyId, lineIds);
        Map<UUID, SaleLine> infos = lookup.findAll(companyId, lineIds);
        int perDay = access.settings(companyId).getMinutesPerDay();
        for (UUID lineId : lineIds) {
            SaleLine info = infos.get(lineId);
            if (info == null || !info.eligible() || info.closed()) {
                continue;   // stays in the "needs attention" list; no quantity changes (TSH-06 #6, #8)
            }
            BigDecimal qty = BillingQuantity.toQuantity(minutes.getOrDefault(lineId, 0L), info.unit(), perDay);
            try {
                billing.setDelivered(companyId, lineId, qty, "Timesheets: " + source);
                updated.add(lineId);
                audit.recordBusinessEvent(companyId, AUDIT_MODEL, lineId, "Delivered quantity recomputed",
                        Map.of("quantity", qty.toPlainString(), "unit", info.unit(), "source", source));
            } catch (DomainException ex) {
                // Approval must stand even if Sales refuses; the entries stay visible in the needs-attention list.
                audit.recordBusinessEvent(companyId, AUDIT_MODEL, lineId, "Delivered quantity could not be set",
                        Map.of("error", String.valueOf(ex.getMessage())));
            }
        }
        return updated;
    }

    /** NOT_BILLABLE, PENDING_APPROVAL, NO_LINE, ORDER_CLOSED, TO_INVOICE, PARTIAL or INVOICED (TSH-06 #9). */
    Map<UUID, String> statuses(CompanyId companyId, Collection<Entry> list, Map<UUID, WeekStatus> weekStatus) {
        Map<UUID, String> out = new HashMap<>();
        Set<UUID> lines = new LinkedHashSet<>();
        for (Entry e : list) {
            if (!e.isBillable()) {
                out.put(e.getId().getId(), "NOT_BILLABLE");
            } else if (weekStatus.get(e.getWeekId().getId()) != WeekStatus.APPROVED) {
                out.put(e.getId().getId(), "PENDING_APPROVAL");
            } else if (e.getSaleLineId() == null) {
                out.put(e.getId().getId(), "NO_LINE");
            } else {
                lines.add(e.getSaleLineId());
            }
        }
        if (lines.isEmpty()) {
            return out;
        }
        Map<UUID, SaleLine> infos = lookup.findAll(companyId, lines);
        int perDay = access.settings(companyId).getMinutesPerDay();
        Map<UUID, List<Entry>> byLine = entries.findApprovedBillableByLines(companyId, lines).stream()
                .collect(Collectors.groupingBy(Entry::getSaleLineId));
        for (UUID lineId : lines) {
            SaleLine info = infos.get(lineId);
            List<Entry> fifo = byLine.getOrDefault(lineId, List.of()).stream()
                    .sorted(Comparator.comparing(Entry::getWorkDate).thenComparing(Entry::getCreatedAt)
                            .thenComparing(e -> e.getId().getId())).toList();
            long invoiced = info == null ? 0 : BillingQuantity.toMinutes(info.qtyInvoiced(), info.unit(), perDay);
            for (Entry e : fifo) {
                long allocated = Math.min(e.getMinutes(), Math.max(0, invoiced));
                invoiced -= allocated;
                String status = info != null && info.closed() ? "ORDER_CLOSED"
                        : allocated >= e.getMinutes() ? "INVOICED" : allocated > 0 ? "PARTIAL" : "TO_INVOICE";
                out.put(e.getId().getId(), status);
            }
        }
        for (Entry e : list) {
            out.putIfAbsent(e.getId().getId(), "TO_INVOICE");
        }
        return out;
    }
}
