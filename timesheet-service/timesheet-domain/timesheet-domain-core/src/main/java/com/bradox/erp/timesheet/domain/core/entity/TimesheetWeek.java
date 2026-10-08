package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One employee's entries for one week, with a status (TSH-05). DRAFT, then SUBMITTED, then APPROVED or REFUSED;
 * a refused week goes back to editing and can be submitted again; an approved week can be reopened.
 */
public class TimesheetWeek extends AggregateRoot<WeekId> {

    private CompanyId companyId;
    private UUID employeeId;
    private LocalDate weekStart;
    private WeekStatus status;
    private boolean locked;
    private Instant createdAt;
    private Instant submittedAt;
    private String approvedBy;
    private Instant approvedAt;
    private String refusedReason;

    private TimesheetWeek() {
    }

    public static TimesheetWeek open(WeekId id, CompanyId companyId, UUID employeeId, LocalDate weekStart,
                                     Instant now) {
        TimesheetWeek w = new TimesheetWeek();
        w.setId(id);
        w.companyId = companyId;
        w.employeeId = employeeId;
        w.weekStart = weekStart;
        w.status = WeekStatus.DRAFT;
        w.createdAt = now;
        return w;
    }

    public static TimesheetWeek restore(WeekId id, CompanyId companyId, UUID employeeId, LocalDate weekStart,
                                        WeekStatus status, boolean locked, Instant createdAt) {
        return restore(id, companyId, employeeId, weekStart, status, locked, createdAt, null, null, null, null);
    }

    public static TimesheetWeek restore(WeekId id, CompanyId companyId, UUID employeeId, LocalDate weekStart,
                                        WeekStatus status, boolean locked, Instant createdAt, Instant submittedAt,
                                        String approvedBy, Instant approvedAt, String refusedReason) {
        TimesheetWeek w = new TimesheetWeek();
        w.setId(id);
        w.companyId = companyId;
        w.employeeId = employeeId;
        w.weekStart = weekStart;
        w.status = status;
        w.locked = locked;
        w.createdAt = createdAt;
        w.submittedAt = submittedAt;
        w.approvedBy = approvedBy;
        w.approvedAt = approvedAt;
        w.refusedReason = refusedReason;
        return w;
    }

    /** BR-TSH-05: only DRAFT or REFUSED weeks that are not locked accept changes. */
    public boolean isEditable() {
        return !locked && (status == WeekStatus.DRAFT || status == WeekStatus.REFUSED);
    }

    /** BR-TSH-15: the whole week is submitted at once. Emptiness is checked by the caller, who knows the entries. */
    public void submit(Instant now) {
        if (!isEditable()) {
            throw new TimesheetDomainException("error.timesheet.weekNotSubmittable", new Object[]{status},
                    "This week cannot be submitted because it is " + (locked ? "locked" : status.name().toLowerCase()));
        }
        status = WeekStatus.SUBMITTED;
        submittedAt = now;
        refusedReason = null;
    }

    /** {@code fromDraft} is for companies that turned approval off: the week goes straight to APPROVED. */
    public void approve(String by, Instant now, boolean fromDraft) {
        boolean ok = status == WeekStatus.SUBMITTED || (fromDraft && status == WeekStatus.DRAFT);
        if (!ok) {
            throw new TimesheetDomainException("error.timesheet.weekNotApprovable", new Object[]{status},
                    "Only a submitted week can be approved");
        }
        status = WeekStatus.APPROVED;
        approvedBy = by;
        approvedAt = now;
        refusedReason = null;
    }

    public void refuse(String reason) {
        if (status != WeekStatus.SUBMITTED) {
            throw new TimesheetDomainException("error.timesheet.weekNotApprovable", new Object[]{status},
                    "Only a submitted week can be refused");
        }
        if (reason == null || reason.isBlank()) {
            throw new TimesheetDomainException("error.timesheet.reasonRequired", null, "A reason is required");
        }
        status = WeekStatus.REFUSED;
        refusedReason = reason.trim();
        approvedBy = null;
        approvedAt = null;
    }

    public void reopen(String reason) {
        if (status != WeekStatus.APPROVED) {
            throw new TimesheetDomainException("error.timesheet.weekNotApproved", new Object[]{status},
                    "Only an approved week can be reopened");
        }
        if (reason == null || reason.isBlank()) {
            throw new TimesheetDomainException("error.timesheet.reasonRequired", null, "A reason is required");
        }
        status = WeekStatus.DRAFT;
        approvedBy = null;
        approvedAt = null;
        locked = false;
    }

    public void lock() {
        locked = true;
    }

    public CompanyId getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public LocalDate getWeekStart() { return weekStart; }
    public WeekStatus getStatus() { return status; }
    public boolean isLocked() { return locked; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRefusedReason() { return refusedReason; }
}
