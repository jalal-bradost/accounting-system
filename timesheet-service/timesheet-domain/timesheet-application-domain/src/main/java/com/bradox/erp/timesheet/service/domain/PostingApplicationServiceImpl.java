package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.dto.BackfillResponse;
import com.bradox.erp.timesheet.service.domain.dto.PostingResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.PostingApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort.EmployeeRef;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.service.domain.dto.IntegrityResponse;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import java.math.BigDecimal;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class PostingApplicationServiceImpl implements PostingApplicationService {

    private final PostingRepository postings;
    private final WeekRepository weeks;
    private final ProjectRepository projects;
    private final EmployeeLookupPort employees;
    private final PostingCoordinator coordinator;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final EntryRepository entries;
    private final LaborCostPostingPort ledger;

    PostingApplicationServiceImpl(PostingRepository postings, WeekRepository weeks, ProjectRepository projects,
                                  EmployeeLookupPort employees, PostingCoordinator coordinator, AuditLogPort audit,
                                  TimesheetAccess access, EntryRepository entries, LaborCostPostingPort ledger) {
        this.postings = postings;
        this.weeks = weeks;
        this.projects = projects;
        this.employees = employees;
        this.coordinator = coordinator;
        this.audit = audit;
        this.access = access;
        this.entries = entries;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public PostingResponse bySource(CompanyId companyId, UUID journalEntryId) {
        access.require(TimesheetPermissions.POSTING_MANAGE);
        return postings.findByJournalEntry(companyId, journalEntryId)
                .map(p -> toResponses(companyId, List.of(p)).get(0)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrityResponse integrity(CompanyId companyId) {
        access.require(TimesheetPermissions.POSTING_MANAGE);
        return check(companyId);
    }

    /** Also used by the daily job, which has no signed-in user. */
    IntegrityResponse check(CompanyId companyId) {
        List<IntegrityResponse.Issue> issues = new ArrayList<>();
        String base = ledger.baseCurrency(companyId);
        List<WeekPosting> posted = postings.search(companyId, List.of(PostingStatus.POSTED));
        Map<WeekId, TimesheetWeek> weekById = weeks.findByIds(posted.stream().map(WeekPosting::getWeekId).distinct().toList())
                .stream().collect(Collectors.toMap(TimesheetWeek::getId, w -> w));
        Map<UUID, EmployeeRef> people = employees.findAll(companyId,
                weekById.values().stream().map(TimesheetWeek::getEmployeeId).collect(Collectors.toSet()));
        java.util.function.Function<TimesheetWeek, String> who = w -> {
            EmployeeRef e = people.get(w.getEmployeeId());
            return e == null ? null : e.name();
        };
        for (WeekPosting p : posted) {
            TimesheetWeek w = weekById.get(p.getWeekId());
            if (w == null) {
                continue;
            }
            if (w.getStatus() != WeekStatus.APPROVED) {
                issues.add(new IntegrityResponse.Issue("POSTED_NOT_APPROVED", w.getId().getId(), who.apply(w), w.getWeekStart(),
                        "Posted to the ledger but the week is " + w.getStatus().name().toLowerCase()));
                continue;
            }
            BigDecimal lineSum = p.getLines().stream().map(WeekPosting.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (lineSum.subtract(p.getTotalAmount()).abs().compareTo(new BigDecimal("0.0001")) > 0) {
                issues.add(new IntegrityResponse.Issue("LINES_MISMATCH", w.getId().getId(), who.apply(w), w.getWeekStart(),
                        "Lines add up to " + lineSum.toPlainString() + " but the entry is " + p.getTotalAmount().toPlainString()));
            }
            var weekEntries = entries.findByWeek(w.getId());
            boolean allBase = weekEntries.stream().allMatch(e -> e.getCostCurrency() == null || e.getCostCurrency().equalsIgnoreCase(base));
            if (allBase) {
                BigDecimal cost = weekEntries.stream().filter(e -> !e.isCostMissing() && e.getCostAmount() != null)
                        .map(e -> e.getCostAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (cost.subtract(p.getTotalAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    issues.add(new IntegrityResponse.Issue("COST_MISMATCH", w.getId().getId(), who.apply(w), w.getWeekStart(),
                            "Approved cost is " + cost.toPlainString() + " but the ledger has " + p.getTotalAmount().toPlainString()));
                }
            }
        }
        java.time.Instant recent = access.clock().instant().minusSeconds(3600);   // a just-approved week may still be queued
        for (WeekId id : postings.approvedWeeksWithoutActivePosting(companyId)) {
            TimesheetWeek w = weeks.findById(id).orElse(null);
            if (w == null || (w.getApprovedAt() != null && w.getApprovedAt().isAfter(recent))) {
                continue;
            }
            BigDecimal cost = entries.findByWeek(id).stream().filter(e -> !e.isCostMissing() && e.getCostAmount() != null)
                    .map(e -> e.getCostAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (cost.signum() > 0) {
                String name = employees.find(companyId, w.getEmployeeId()).map(EmployeeRef::name).orElse(null);
                issues.add(new IntegrityResponse.Issue("MISSING_POSTING", id.getId(), name, w.getWeekStart(),
                        "Approved with cost " + cost.toPlainString() + " but nothing is posted. Run backfill."));
            }
        }
        return new IntegrityResponse(issues.isEmpty(), issues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostingResponse> list(CompanyId companyId, List<PostingStatus> statuses) {
        access.require(TimesheetPermissions.POSTING_MANAGE);
        return toResponses(companyId, postings.search(companyId, statuses));
    }

    /**
     * Not one big transaction on purpose: each step commits before the next reads it, because posting runs in its own
     * transaction and has to see the row (and so that an accounting failure is stored, not rolled back).
     */
    @Override
    public PostingResponse retry(CompanyId companyId, UUID postingId) {
        access.require(TimesheetPermissions.POSTING_MANAGE);
        WeekPosting p = postings.find(new PostingId(postingId)).filter(x -> x.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Posting not found"));
        if (p.getStatus() == PostingStatus.FAILED) {
            coordinator.resetForRetryCommitted(postingId);
        } else if (p.getStatus() != PostingStatus.PENDING) {
            throw new TimesheetDomainException("error.timesheet.postingNotFailed", null, "Only a failed or pending posting can be retried");
        }
        audit.recordBusinessEvent(companyId, PostingCoordinator.AUDIT_MODEL, postingId, "Posting retried by hand",
                Map.of("by", access.actorLabel()));
        coordinator.processNow(postingId);
        return toResponses(companyId, List.of(postings.find(new PostingId(postingId)).orElse(p))).get(0);
    }

    @Override
    public BackfillResponse backfill(CompanyId companyId) {
        access.require(TimesheetPermissions.POSTING_MANAGE);
        if (!access.settings(companyId).isLedgerPostingEnabled()) {
            throw new TimesheetDomainException("error.timesheet.postingDisabled", null,
                    "Turn on ledger posting in the settings first");
        }
        List<BackfillResponse.Item> items = new ArrayList<>();
        int posted = 0;
        int failed = 0;
        int skipped = 0;
        for (WeekId weekId : postings.approvedWeeksWithoutActivePosting(companyId)) {
            TimesheetWeek week = weeks.findById(weekId).orElse(null);
            UUID createdId = coordinator.enqueueCommitted(companyId, weekId);
            if (week == null || createdId == null) {
                continue;
            }
            coordinator.processNow(createdId);
            WeekPosting result = postings.find(new PostingId(createdId)).orElseThrow();
            String name = employees.find(companyId, week.getEmployeeId()).map(EmployeeRef::name).orElse(null);
            items.add(new BackfillResponse.Item(weekId.getId(), name, week.getWeekStart(), result.getStatus(), result.getErrorMessage()));
            switch (result.getStatus()) {
                case POSTED -> posted++;
                case SKIPPED -> skipped++;
                default -> failed++;
            }
        }
        audit.recordBusinessEvent(companyId, PostingCoordinator.AUDIT_MODEL, companyId.getId(), "Backfill run",
                Map.of("posted", posted, "failed", failed, "skipped", skipped, "by", access.actorLabel()));
        return new BackfillResponse(items, posted, failed, skipped);
    }

    private List<PostingResponse> toResponses(CompanyId companyId, List<WeekPosting> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<WeekId, TimesheetWeek> weekById = weeks.findByIds(list.stream().map(WeekPosting::getWeekId).distinct().toList())
                .stream().collect(Collectors.toMap(TimesheetWeek::getId, w -> w));
        Map<UUID, EmployeeRef> people = employees.findAll(companyId,
                weekById.values().stream().map(TimesheetWeek::getEmployeeId).collect(Collectors.toSet()));
        Map<ProjectId, Project> projectById = projects.findByIds(list.stream().flatMap(p -> p.getLines().stream())
                .map(l -> new ProjectId(l.projectId())).distinct().toList()).stream().collect(Collectors.toMap(Project::getId, p -> p));
        return list.stream().map(p -> {
            TimesheetWeek w = weekById.get(p.getWeekId());
            EmployeeRef emp = w == null ? null : people.get(w.getEmployeeId());
            return new PostingResponse(p.getId().getId(), p.getWeekId().getId(), w == null ? null : w.getEmployeeId(),
                    emp == null ? null : emp.name(), w == null ? null : w.getWeekStart(), p.getVersion(), p.getStatus(),
                    p.getJournalEntryId(), p.getReversalEntryId(), p.getEntryDate(), p.isLatePosted(), p.getTotalAmount(),
                    p.getAttempts(), p.getErrorMessage(), p.getPostedAt(), p.getLines().stream().map(l -> {
                        Project pr = projectById.get(new ProjectId(l.projectId()));
                        return new PostingResponse.Line(l.projectId(), pr == null ? null : pr.getName(), l.debitAccountId(),
                                l.amount(), l.minutes());
                    }).toList());
        }).sorted(Comparator.comparing(PostingResponse::weekStart, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PostingResponse::version, Comparator.reverseOrder())).toList();
    }
}
