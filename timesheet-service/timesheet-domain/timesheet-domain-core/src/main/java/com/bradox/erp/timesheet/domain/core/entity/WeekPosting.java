package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The ledger posting of one approved employee-week (TSH-10). A week has at most one active (non-reversed) posting;
 * a reopen reverses it and a re-approval makes a new version (BR-TSH-16).
 */
public class WeekPosting extends AggregateRoot<PostingId> {

    public static final int MAX_ATTEMPTS = 5;

    public record Line(UUID projectId, UUID debitAccountId, BigDecimal amount, int minutes) {
    }

    private CompanyId companyId;
    private WeekId weekId;
    private int version;
    private PostingStatus status;
    private UUID journalEntryId;
    private UUID reversalEntryId;
    private LocalDate entryDate;
    private boolean latePosted;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private int attempts;
    private String errorMessage;
    private Instant createdAt;
    private Instant postedAt;
    private List<Line> lines = new ArrayList<>();

    private WeekPosting() {
    }

    public static WeekPosting pending(PostingId id, CompanyId companyId, WeekId weekId, int version, Instant now) {
        WeekPosting p = new WeekPosting();
        p.setId(id);
        p.companyId = companyId;
        p.weekId = weekId;
        p.version = version;
        p.status = PostingStatus.PENDING;
        p.createdAt = now;
        return p;
    }

    public static WeekPosting restore(PostingId id, CompanyId companyId, WeekId weekId, int version, PostingStatus status,
                                      UUID journalEntryId, UUID reversalEntryId, LocalDate entryDate, boolean latePosted,
                                      BigDecimal totalAmount, int attempts, String errorMessage, Instant createdAt,
                                      Instant postedAt, List<Line> lines) {
        WeekPosting p = new WeekPosting();
        p.setId(id);
        p.companyId = companyId;
        p.weekId = weekId;
        p.version = version;
        p.status = status;
        p.journalEntryId = journalEntryId;
        p.reversalEntryId = reversalEntryId;
        p.entryDate = entryDate;
        p.latePosted = latePosted;
        p.totalAmount = totalAmount;
        p.attempts = attempts;
        p.errorMessage = errorMessage;
        p.createdAt = createdAt;
        p.postedAt = postedAt;
        p.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        return p;
    }

    /** True while this posting still counts for the week: everything except REVERSED. */
    public boolean isActive() {
        return status != PostingStatus.REVERSED;
    }

    public boolean canProcess() {
        return status == PostingStatus.PENDING || (status == PostingStatus.FAILED && attempts < MAX_ATTEMPTS);
    }

    public void markPosted(UUID journalEntryId, LocalDate entryDate, boolean late, BigDecimal total, List<Line> lines,
                           Instant now) {
        requireProcessable();
        this.status = PostingStatus.POSTED;
        this.journalEntryId = journalEntryId;
        this.entryDate = entryDate;
        this.latePosted = late;
        this.totalAmount = total;
        this.lines = new ArrayList<>(lines);
        this.attempts++;
        this.errorMessage = null;
        this.postedAt = now;
    }

    public void markSkipped() {
        requireProcessable();
        this.status = PostingStatus.SKIPPED;
        this.totalAmount = BigDecimal.ZERO;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        requireProcessable();
        this.status = PostingStatus.FAILED;
        this.attempts++;
        this.errorMessage = message == null ? null : message.length() > 900 ? message.substring(0, 900) : message;
    }

    /** A manual retry gives a failed posting a fresh set of attempts (TSH-10 #5). */
    public void resetForRetry() {
        if (status != PostingStatus.FAILED) {
            throw new TimesheetDomainException("error.timesheet.postingNotFailed", null, "Only a failed posting can be retried");
        }
        this.attempts = 0;
    }

    public void markReversed(UUID reversalEntryId) {
        if (status == PostingStatus.REVERSED) {
            throw new TimesheetDomainException("error.timesheet.postingAlreadyReversed", null, "Already reversed");
        }
        this.status = PostingStatus.REVERSED;
        this.reversalEntryId = reversalEntryId;
    }

    private void requireProcessable() {
        if (status != PostingStatus.PENDING && status != PostingStatus.FAILED) {
            throw new TimesheetDomainException("error.timesheet.postingNotProcessable", new Object[]{status},
                    "This posting is already " + status.name().toLowerCase());
        }
    }

    public CompanyId getCompanyId() { return companyId; }
    public WeekId getWeekId() { return weekId; }
    public int getVersion() { return version; }
    public PostingStatus getStatus() { return status; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public UUID getReversalEntryId() { return reversalEntryId; }
    public LocalDate getEntryDate() { return entryDate; }
    public boolean isLatePosted() { return latePosted; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getAttempts() { return attempts; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPostedAt() { return postedAt; }
    public List<Line> getLines() { return List.copyOf(lines); }
}
