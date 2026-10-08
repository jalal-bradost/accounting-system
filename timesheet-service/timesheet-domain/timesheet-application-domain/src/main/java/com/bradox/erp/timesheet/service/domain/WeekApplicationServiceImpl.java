package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.exception.DomainException;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.ForbiddenException;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.CostCalculator;
import com.bradox.erp.timesheet.domain.core.rule.WeekCalendar;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveCommand;
import com.bradox.erp.timesheet.service.domain.dto.BulkApproveResponse;
import com.bradox.erp.timesheet.service.domain.dto.SubmitWeekCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekReasonCommand;
import com.bradox.erp.timesheet.service.domain.dto.WeekSummaryResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.WeekApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort;
import com.bradox.erp.timesheet.service.domain.ports.output.ExpectedHoursPort.ExpectedDay;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Submit, approve, refuse and reopen weeks (TSH-05), with the cost snapshot taken at approval (D6). */
@Service
@Validated
class WeekApplicationServiceImpl implements WeekApplicationService {

    static final String AUDIT_MODEL = "tsh.week";
    static final int LIST_LIMIT = 300;

    private final WeekRepository weeks;
    private final EntryRepository entries;
    private final EmployeeLookupPort employees;
    private final ExpectedHoursPort expectedHours;
    private final LaborCostPort laborCost;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final BillingSync billingSync;
    private final PostingCoordinator postingCoordinator;
    private final PostingRepository postings;
    private final TimesheetNotifier notifier;

    WeekApplicationServiceImpl(WeekRepository weeks, EntryRepository entries, EmployeeLookupPort employees,
                               ExpectedHoursPort expectedHours, LaborCostPort laborCost, AuditLogPort audit,
                               TimesheetAccess access, BillingSync billingSync, PostingCoordinator postingCoordinator,
                               PostingRepository postings, TimesheetNotifier notifier) {
        this.weeks = weeks;
        this.entries = entries;
        this.employees = employees;
        this.expectedHours = expectedHours;
        this.laborCost = laborCost;
        this.audit = audit;
        this.access = access;
        this.billingSync = billingSync;
        this.postingCoordinator = postingCoordinator;
        this.postings = postings;
        this.notifier = notifier;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeekSummaryResponse> list(CompanyId companyId, Scope scope, WeekStatus status, LocalDate from, LocalDate to) {
        List<WeekStatus> statuses = status == null ? null : List.of(status);
        Set<UUID> employeeIds;
        switch (scope) {
            case APPROVE -> {
                if (!access.can(TimesheetPermissions.APPROVE) && !access.can(TimesheetPermissions.APPROVE_ALL)) {
                    access.require(TimesheetPermissions.APPROVE);
                }
                statuses = List.of(WeekStatus.SUBMITTED);
                employeeIds = access.can(TimesheetPermissions.APPROVE_ALL) ? null : access.teamIds(companyId);
            }
            case ALL -> {
                access.require(TimesheetPermissions.ENTRY_VIEW_ALL);
                employeeIds = null;
            }
            case TEAM -> {
                if (!access.can(TimesheetPermissions.ENTRY_VIEW_ALL)) {
                    access.require(TimesheetPermissions.ENTRY_VIEW_TEAM);
                }
                employeeIds = access.teamIds(companyId);
            }
            default -> employeeIds = access.actor(companyId).map(a -> Set.of(a.id())).orElse(Set.of());
        }
        if (employeeIds != null && employeeIds.isEmpty()) {
            return List.of();
        }
        List<TimesheetWeek> found = weeks.search(companyId, statuses, employeeIds, from, to);
        return summaries(companyId, found.size() > LIST_LIMIT ? found.subList(0, LIST_LIMIT) : found);
    }

    @Override
    @Transactional(readOnly = true)
    public WeekSummaryResponse get(CompanyId companyId, UUID weekId) {
        TimesheetWeek w = load(companyId, weekId);
        requireCanSee(companyId, w);
        return summaries(companyId, List.of(w)).get(0);
    }

    @Override
    @Transactional
    public WeekSummaryResponse submit(CompanyId companyId, SubmitWeekCommand c) {
        var target = access.resolveForWrite(companyId, c.employeeId());
        TimesheetSettings settings = access.settings(companyId);
        LocalDate start = WeekCalendar.weekStart(c.date(), settings.getWeekStartDay());
        TimesheetWeek week = weeks.find(companyId, target.employee().id(), start).orElse(null);
        List<Entry> weekEntries = week == null ? List.of() : entries.findByWeek(week.getId());
        if (weekEntries.isEmpty()) {
            throw new TimesheetDomainException("error.timesheet.emptyWeek", null,
                    "There is no time in this week, so there is nothing to submit");
        }
        Instant now = access.clock().instant();
        week.submit(now);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, week.getId().getId(), "Week submitted",
                Map.of("employeeId", week.getEmployeeId().toString(), "weekStart", start.toString(),
                        "minutes", weekEntries.stream().mapToInt(Entry::getMinutes).sum()));
        if (!settings.isApprovalRequired()) {
            // TSH-05 #2: with approval off, submitting approves straight away with the same locking.
            week.approve("system", now, false);
            weeks.save(week);
            snapshotCosts(companyId, week, weekEntries);
            billingSync.onApproved(companyId, week, weekEntries);
            postingCoordinator.onApproved(companyId, week);
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, week.getId().getId(), "Week auto-approved", Map.of());
        } else {
            notifier.weekSubmitted(companyId, week, weekEntries.stream().mapToInt(Entry::getMinutes).sum());
        }
        return summaries(companyId, List.of(weeks.save(week))).get(0);
    }

    @Override
    @Transactional
    public WeekSummaryResponse approve(CompanyId companyId, UUID weekId) {
        return summaries(companyId, List.of(approveOne(companyId, weekId))).get(0);
    }

    @Override
    @Transactional
    public WeekSummaryResponse refuse(CompanyId companyId, UUID weekId, WeekReasonCommand command) {
        TimesheetWeek w = load(companyId, weekId);
        requireCanApprove(companyId, w);
        w.refuse(command.reason());
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, weekId, "Week refused", Map.of("reason", w.getRefusedReason()));
        notifier.approvalDecided(companyId, w);
        notifier.weekRefused(companyId, w, w.getRefusedReason());
        return summaries(companyId, List.of(weeks.save(w))).get(0);
    }

    @Override
    @Transactional
    public WeekSummaryResponse reopen(CompanyId companyId, UUID weekId, WeekReasonCommand command) {
        access.require(TimesheetPermissions.REOPEN);
        TimesheetWeek w = load(companyId, weekId);
        List<Entry> weekEntries = entries.findByWeek(w.getId());
        if (w.getStatus() == WeekStatus.APPROVED) {
            billingSync.checkReopenAllowed(companyId, weekEntries);      // BR-TSH-14
            postingCoordinator.onReopened(companyId, w, command.reason() == null ? "" : command.reason().trim());   // BR-TSH-16
        }
        w.reopen(command.reason());
        TimesheetWeek saved = weeks.save(w);
        weekEntries.forEach(e -> e.clearCost());
        billingSync.onReopened(companyId, weekEntries);
        weekEntries.forEach(entries::save);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, weekId, "Week reopened", Map.of("reason", command.reason().trim()));
        notifier.weekReopened(companyId, saved, command.reason().trim());
        return summaries(companyId, List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public BulkApproveResponse bulkApprove(CompanyId companyId, BulkApproveCommand command) {
        List<BulkApproveResponse.Item> items = new ArrayList<>();
        int ok = 0;
        for (UUID id : command.weekIds()) {
            try {
                approveOne(companyId, id);
                items.add(new BulkApproveResponse.Item(id, true, null));
                ok++;
            } catch (DomainException | ResponseStatusException e) {
                items.add(new BulkApproveResponse.Item(id, false,
                        e instanceof ResponseStatusException r ? r.getReason() : e.getMessage()));
            }
        }
        return new BulkApproveResponse(items, ok, items.size() - ok);
    }

    private TimesheetWeek approveOne(CompanyId companyId, UUID weekId) {
        TimesheetWeek w = load(companyId, weekId);
        requireCanApprove(companyId, w);
        w.approve(access.actorLabel(), access.clock().instant(), false);
        TimesheetWeek saved = weeks.save(w);   // saved first: the billing totals count approved weeks only
        List<Entry> weekEntries = entries.findByWeek(w.getId());
        snapshotCosts(companyId, w, weekEntries);
        billingSync.onApproved(companyId, w, weekEntries);
        postingCoordinator.onApproved(companyId, w);
        notifier.approvalDecided(companyId, w);
        notifier.weekApproved(companyId, w);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, weekId, "Week approved",
                Map.of("employeeId", w.getEmployeeId().toString(), "weekStart", w.getWeekStart().toString()));
        return saved;
    }

    /** D6: price every entry once, at approval. A missing rate flags the entry instead of guessing (TSH-07 #6). */
    private void snapshotCosts(CompanyId companyId, TimesheetWeek week, List<Entry> weekEntries) {
        var rate = laborCost.hourlyRate(companyId, week.getEmployeeId(), week.getWeekStart().plusDays(6)).orElse(null);
        for (Entry e : weekEntries) {
            if (rate == null) {
                e.applyCost(null, null, java.math.BigDecimal.ZERO, true);
            } else {
                e.applyCost(rate.perHour(), rate.currency(), CostCalculator.cost(e.getMinutes(), rate.perHour()), false);
            }
            entries.save(e);
        }
    }

    private TimesheetWeek load(CompanyId companyId, UUID weekId) {
        return weeks.findById(new WeekId(weekId)).filter(w -> w.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Week not found"));
    }

    private void requireCanApprove(CompanyId companyId, TimesheetWeek w) {
        EmployeeRef owner = employees.find(companyId, w.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (!access.canApprove(companyId, owner, access.settings(companyId))) {
            throw new ForbiddenException("error.timesheet.cannotApprove", null,
                    "You cannot approve this week. Approvers are the employee's manager, the department manager "
                            + "or a Timesheet Manager, and nobody approves their own week.");
        }
    }

    private void requireCanSee(CompanyId companyId, TimesheetWeek w) {
        boolean mine = access.actor(companyId).map(a -> a.id().equals(w.getEmployeeId())).orElse(false);
        if (!mine && !access.can(TimesheetPermissions.ENTRY_VIEW_ALL)
                && !(access.can(TimesheetPermissions.ENTRY_VIEW_TEAM) && access.teamIds(companyId).contains(w.getEmployeeId()))
                && !(access.can(TimesheetPermissions.APPROVE) && access.teamIds(companyId).contains(w.getEmployeeId()))) {
            access.require(TimesheetPermissions.ENTRY_VIEW_ALL);
        }
    }

    private List<WeekSummaryResponse> summaries(CompanyId companyId, List<TimesheetWeek> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        TimesheetSettings settings = access.settings(companyId);
        Map<UUID, EmployeeRef> owners = employees.findAll(companyId,
                list.stream().map(TimesheetWeek::getEmployeeId).distinct().toList());
        Map<UUID, Long> totals = entries.minutesByWeek(list.stream().map(TimesheetWeek::getId).toList());
        boolean canReopen = access.can(TimesheetPermissions.REOPEN);
        boolean seesPostings = access.can(TimesheetPermissions.POSTING_MANAGE);
        Map<UUID, WeekPosting> latest = seesPostings
                ? postings.findLatestByWeeks(list.stream().map(TimesheetWeek::getId).toList()) : Map.of();
        Map<UUID, WeekSummaryResponse> byId = new LinkedHashMap<>();
        for (TimesheetWeek w : list) {
            EmployeeRef owner = owners.get(w.getEmployeeId());
            LocalDate end = w.getWeekStart().plusDays(6);
            int expected = expectedHours.expected(companyId, w.getEmployeeId(), w.getWeekStart(), end).values().stream()
                    .mapToInt(ExpectedDay::minutes).sum();
            int total = totals.getOrDefault(w.getId().getId(), 0L).intValue();
            byId.put(w.getId().getId(), new WeekSummaryResponse(w.getId().getId(), w.getEmployeeId(),
                    owner == null ? null : owner.name(), w.getWeekStart(), end, w.getStatus(), w.isLocked(), total,
                    expected, expected > 0 ? Math.max(0, total - expected) : 0, w.getSubmittedAt(), w.getApprovedBy(), w.getApprovedAt(),
                    w.getRefusedReason(),
                    w.getStatus() == WeekStatus.SUBMITTED && owner != null && access.canApprove(companyId, owner, settings),
                    w.getStatus() == WeekStatus.APPROVED && canReopen,
                    latest.containsKey(w.getId().getId()) ? latest.get(w.getId().getId()).getStatus() : null,
                    latest.containsKey(w.getId().getId()) ? latest.get(w.getId().getId()).getErrorMessage() : null,
                    latest.containsKey(w.getId().getId()) && latest.get(w.getId().getId()).isLatePosted()));
        }
        return byId.values().stream().sorted(Comparator.comparing(WeekSummaryResponse::weekStart).reversed()
                .thenComparing(r -> r.employeeName() == null ? "" : r.employeeName().toLowerCase())).toList();
    }
}
