package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetSettings;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.PostingBuilder;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.ports.output.EmployeeLookupPort;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPostingPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Posts approved weeks to the ledger (TSH-10). Approval never depends on accounting being available: the posting row
 * is created inside the approval transaction and processed after it commits, in its own transaction; a failure is
 * stored on the row and retried by the hourly job or by hand (NFR reliability).
 */
@Component
class PostingCoordinator {

    static final String AUDIT_MODEL = "tsh.posting";
    static final int MONETARY_SCALE = 4;

    private final PostingRepository postings;
    private final WeekRepository weeks;
    private final EntryRepository entries;
    private final ProjectRepository projects;
    private final EmployeeLookupPort employees;
    private final LaborCostPostingPort ledger;
    private final AuditLogPort audit;
    private final TimesheetAccess access;
    private final ObjectProvider<PlatformTransactionManager> txManager;

    PostingCoordinator(PostingRepository postings, WeekRepository weeks, EntryRepository entries, ProjectRepository projects,
                       EmployeeLookupPort employees, LaborCostPostingPort ledger, AuditLogPort audit, TimesheetAccess access,
                       ObjectProvider<PlatformTransactionManager> txManager) {
        this.postings = postings;
        this.weeks = weeks;
        this.entries = entries;
        this.projects = projects;
        this.employees = employees;
        this.ledger = ledger;
        this.audit = audit;
        this.access = access;
        this.txManager = txManager;
    }

    /** Called inside the approval transaction. Creates the queued posting; never throws for accounting reasons. */
    void onApproved(CompanyId companyId, TimesheetWeek week) {
        TimesheetSettings settings = access.settings(companyId);
        if (!settings.isLedgerPostingEnabled()) {
            return;
        }
        WeekPosting pending = enqueue(companyId, week.getId());
        if (pending != null) {
            scheduleAfterCommit(pending.getId().getId(), companyId);
        }
    }

    /** Creates a PENDING posting unless the week already has an active one (BR-TSH-16). */
    WeekPosting enqueue(CompanyId companyId, WeekId weekId) {
        if (postings.findActiveByWeek(weekId).isPresent()) {
            return null;
        }
        return postings.save(WeekPosting.pending(new PostingId(UUID.randomUUID()), companyId, weekId,
                postings.maxVersion(weekId) + 1, access.clock().instant()));
    }

    /**
     * Called inside the reopen transaction. A posted week is reversed in the ledger, and if that fails the reopen is
     * refused with the reason; anything not yet posted is simply marked reversed.
     */
    void onReopened(CompanyId companyId, TimesheetWeek week, String reason) {
        WeekPosting active = postings.findActiveByWeek(week.getId()).orElse(null);
        if (active == null) {
            return;
        }
        UUID reversal = null;
        if (active.getStatus() == PostingStatus.POSTED) {
            try {
                reversal = ledger.reverse(companyId, active.getJournalEntryId(), "Timesheet week reopened: " + reason);
            } catch (RuntimeException ex) {
                throw new TimesheetDomainException("error.timesheet.reversalFailed", new Object[]{ex.getMessage()},
                        "The ledger posting of this week could not be reversed, so the week was not reopened: " + ex.getMessage());
            }
        }
        active.markReversed(reversal);
        postings.save(active);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, active.getId().getId(), "Posting reversed",
                Map.of("weekId", week.getId().getId().toString(), "reason", reason));
    }

    private void scheduleAfterCommit(UUID postingId, CompanyId companyId) {
        Runnable job = () -> {
            try {
                inNewTransaction(() -> {
                    process(postingId);
                    return null;
                });
            } catch (RuntimeException ex) {
                // The posting row keeps the failure; the hourly job retries it. Nothing may break the approval.
                audit.recordBusinessEvent(companyId, AUDIT_MODEL, postingId, "Posting could not be processed",
                        Map.of("error", String.valueOf(ex.getMessage())));
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    /**
     * Creates the queued posting and commits it before anything processes it. Used by backfill, where the processing
     * runs in its own transaction and must be able to see the row.
     */
    UUID enqueueCommitted(CompanyId companyId, WeekId weekId) {
        return inNewTransaction(() -> {
            WeekPosting p = enqueue(companyId, weekId);
            return p == null ? null : p.getId().getId();
        });
    }

    /** A manual retry gives a failed posting fresh attempts, committed before it is processed. */
    void resetForRetryCommitted(UUID postingId) {
        inNewTransaction(() -> {
            postings.find(new PostingId(postingId)).filter(p -> p.getStatus() == PostingStatus.FAILED).ifPresent(p -> {
                p.resetForRetry();
                postings.save(p);
            });
            return null;
        });
    }

    /** Public entry for the retry job and the manual retry. Runs in its own transaction when a manager is available. */
    void processNow(UUID postingId) {
        inNewTransaction(() -> {
            process(postingId);
            return null;
        });
    }

    private <T> T inNewTransaction(Supplier<T> work) {
        PlatformTransactionManager tm = txManager.getIfAvailable();
        if (tm == null) {
            return work.get();
        }
        TransactionTemplate t = new TransactionTemplate(tm);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t.execute(status -> work.get());
    }

    /** Builds the entry from the week's cost snapshots and posts it. Safe to run twice: a POSTED posting is left alone. */
    void process(UUID postingId) {
        WeekPosting posting = postings.find(new PostingId(postingId)).orElse(null);
        if (posting == null || !posting.canProcess()) {
            return;
        }
        CompanyId companyId = posting.getCompanyId();
        TimesheetWeek week = weeks.findById(posting.getWeekId()).orElse(null);
        try {
            if (week == null) {
                throw new TimesheetDomainException("error.timesheet.weekNotFound", null, "The week no longer exists");
            }
            TimesheetSettings settings = access.settings(companyId);
            List<Entry> weekEntries = entries.findByWeek(week.getId());
            Map<ProjectId, Project> projectById = projects.findByIds(
                    weekEntries.stream().map(Entry::getProjectId).distinct().toList()).stream()
                    .collect(Collectors.toMap(Project::getId, p -> p));
            String base = ledger.baseCurrency(companyId);
            List<PostingBuilder.CostLine> costs = new ArrayList<>();
            for (Entry e : weekEntries) {
                if (e.isCostMissing() || e.getCostAmount() == null || e.getCostAmount().signum() <= 0) {
                    continue;
                }
                BigDecimal amount = e.getCostAmount();
                if (e.getCostCurrency() != null && base != null && !e.getCostCurrency().equalsIgnoreCase(base)) {
                    amount = ledger.toBaseCurrency(companyId, amount, e.getCostCurrency(), e.getWorkDate());
                }
                Project p = projectById.get(e.getProjectId());
                UUID debit = p != null && p.getCostAccountId() != null ? p.getCostAccountId() : settings.getDefaultCostAccountId();
                costs.add(new PostingBuilder.CostLine(e.getProjectId().getId(), debit, e.getMinutes(), amount));
            }
            PostingBuilder.Result built = PostingBuilder.build(costs, MONETARY_SCALE);
            if (built.isEmpty()) {
                posting.markSkipped();
                postings.save(posting);
                audit.recordBusinessEvent(companyId, AUDIT_MODEL, postingId, "Nothing to post", Map.of());
                return;
            }
            if (settings.getLaborAppliedAccountId() == null || built.lines().stream().anyMatch(l -> l.debitAccountId() == null)) {
                throw new TimesheetDomainException("error.timesheet.postingAccountsMissing", null,
                        "The labor cost accounts are not configured. Set them in the timesheet settings.");
            }
            String who = employees.find(companyId, week.getEmployeeId()).map(EmployeeLookupPort.EmployeeRef::name).orElse("?");
            String reference = "Timesheet " + week.getWeekStart() + " " + who;
            List<LaborCostPostingPort.DebitLine> debits = built.lines().stream().map(l -> new LaborCostPostingPort.DebitLine(
                    l.debitAccountId(), l.projectId(), l.amount(),
                    "Labor " + week.getWeekStart() + " " + who + " " + projectLabel(projectById.get(new ProjectId(l.projectId()))))).toList();
            LocalDate weekEnd = week.getWeekStart().plusDays(6);
            LaborCostPostingPort.Result result = ledger.post(new LaborCostPostingPort.Request(companyId, settings.getJournalCode(),
                    settings.getLaborAppliedAccountId(), "Labor cost applied " + week.getWeekStart() + " " + who, weekEnd,
                    access.today(settings), reference, debits));
            posting.markPosted(result.journalEntryId(), result.entryDate(), result.late(), built.total(),
                    built.lines().stream().map(l -> new WeekPosting.Line(l.projectId(), l.debitAccountId(), l.amount(), l.minutes())).toList(),
                    access.clock().instant());
            postings.save(posting);
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, postingId, "Posted to the ledger",
                    Map.of("journalEntryId", result.journalEntryId().toString(), "amount", built.total().toPlainString(),
                            "late", result.late()));
        } catch (RuntimeException ex) {
            WeekPosting current = postings.find(new PostingId(postingId)).orElse(posting);
            if (current.canProcess()) {
                current.markFailed(ex.getMessage());
                postings.save(current);
            }
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, postingId, "Posting failed",
                    Map.of("error", String.valueOf(ex.getMessage()), "attempts", current.getAttempts()));
        }
    }

    private static String projectLabel(Project p) {
        return p == null ? "" : p.getCode() != null ? p.getCode() : p.getName();
    }
}
