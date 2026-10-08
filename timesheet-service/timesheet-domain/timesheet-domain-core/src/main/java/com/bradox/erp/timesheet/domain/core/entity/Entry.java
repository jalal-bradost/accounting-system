package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.rule.DayTotalRules;
import com.bradox.erp.timesheet.domain.core.valueobject.EntryId;
import com.bradox.erp.timesheet.domain.core.valueobject.EntrySource;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One record of time (BR-TSH-01): one employee, one project, optionally one task of that project. */
public class Entry extends AggregateRoot<EntryId> {

    private CompanyId companyId;
    private UUID employeeId;
    private LocalDate workDate;
    private int minutes;
    private ProjectId projectId;
    private TaskId taskId;
    private String description;
    private boolean billable;
    private WeekId weekId;
    private EntrySource source;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private BigDecimal costRate;
    private String costCurrency;
    private BigDecimal costAmount;
    private boolean costMissing;
    private String recordModel;
    private UUID recordId;
    private UUID saleLineId;

    private Entry() {
    }

    public static Entry create(EntryId id, CompanyId companyId, UUID employeeId, LocalDate workDate, int minutes,
                               ProjectId projectId, TaskId taskId, String description, boolean billable,
                               WeekId weekId, EntrySource source, Instant now, String createdBy) {
        if (employeeId == null) {
            throw new TimesheetDomainException("error.timesheet.employeeRequired", null, "An employee is required");
        }
        if (workDate == null) {
            throw new TimesheetDomainException("error.timesheet.dateRequired", null, "A date is required");
        }
        if (projectId == null) {
            throw new TimesheetDomainException("error.timesheet.projectRequired", null, "A project is required");
        }
        validateMinutes(minutes);
        Entry e = new Entry();
        e.setId(id);
        e.companyId = companyId;
        e.employeeId = employeeId;
        e.workDate = workDate;
        e.minutes = minutes;
        e.projectId = projectId;
        e.taskId = taskId;
        e.description = blankToNull(description);
        e.billable = billable;
        e.weekId = weekId;
        e.source = source == null ? EntrySource.MANUAL : source;
        e.createdAt = now;
        e.createdBy = createdBy;
        e.updatedAt = now;
        return e;
    }

    public static Entry restore(EntryId id, CompanyId companyId, UUID employeeId, LocalDate workDate, int minutes,
                                ProjectId projectId, TaskId taskId, String description, boolean billable,
                                WeekId weekId, EntrySource source, Instant createdAt, String createdBy,
                                Instant updatedAt, BigDecimal costRate, String costCurrency, BigDecimal costAmount,
                                boolean costMissing, String recordModel, UUID recordId, UUID saleLineId) {
        Entry e = new Entry();
        e.setId(id);
        e.companyId = companyId;
        e.employeeId = employeeId;
        e.workDate = workDate;
        e.minutes = minutes;
        e.projectId = projectId;
        e.taskId = taskId;
        e.description = description;
        e.billable = billable;
        e.weekId = weekId;
        e.source = source;
        e.createdAt = createdAt;
        e.createdBy = createdBy;
        e.updatedAt = updatedAt;
        e.costRate = costRate;
        e.costCurrency = costCurrency;
        e.costAmount = costAmount;
        e.costMissing = costMissing;
        e.recordModel = recordModel;
        e.recordId = recordId;
        e.saleLineId = saleLineId;
        return e;
    }

    /** The week is re-assigned by the service when the date moves into another week. */
    public void change(LocalDate workDate, int minutes, ProjectId projectId, TaskId taskId, String description,
                       boolean billable, WeekId weekId, Instant now) {
        if (workDate == null) {
            throw new TimesheetDomainException("error.timesheet.dateRequired", null, "A date is required");
        }
        if (projectId == null) {
            throw new TimesheetDomainException("error.timesheet.projectRequired", null, "A project is required");
        }
        validateMinutes(minutes);
        this.workDate = workDate;
        this.minutes = minutes;
        this.projectId = projectId;
        this.taskId = taskId;
        this.description = blankToNull(description);
        this.billable = billable;
        this.weekId = weekId;
        this.updatedAt = now;
    }

    /** Used by the grid, which only ever changes the duration of an existing entry. */
    public void changeMinutes(int minutes, Instant now) {
        validateMinutes(minutes);
        this.minutes = minutes;
        this.updatedAt = now;
    }

    public static void validateMinutes(int minutes) {
        if (minutes < DayTotalRules.MIN_ENTRY_MINUTES || minutes > DayTotalRules.MAX_DAY_MINUTES) {
            throw new TimesheetDomainException("error.timesheet.minutesRange", null,
                    "An entry must be between 1 minute and 24 hours");
        }
    }

    /** D6: the hourly cost is snapshotted at approval so later wage changes never rewrite history. */
    public void applyCost(BigDecimal rate, String currency, BigDecimal amount, boolean missing) {
        this.costRate = rate;
        this.costCurrency = currency;
        this.costAmount = amount;
        this.costMissing = missing;
    }

    public void clearCost() {
        applyCost(null, null, null, false);
    }

    /** TSH-09: an entry logged on a system-managed task remembers the record it belongs to. */
    public void linkRecord(String model, UUID id) {
        this.recordModel = model;
        this.recordId = id;
    }

    /** TSH-06: the sales order line this entry bills to, resolved at approval. Null means "billable, no order line". */
    public void assignSaleLine(UUID saleLineId) {
        this.saleLineId = saleLineId;
    }

    public void useSource(EntrySource source) {
        this.source = source;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public CompanyId getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public int getMinutes() { return minutes; }
    public ProjectId getProjectId() { return projectId; }
    public TaskId getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public boolean isBillable() { return billable; }
    public WeekId getWeekId() { return weekId; }
    public EntrySource getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public BigDecimal getCostRate() { return costRate; }
    public String getCostCurrency() { return costCurrency; }
    public BigDecimal getCostAmount() { return costAmount; }
    public boolean isCostMissing() { return costMissing; }
    public String getRecordModel() { return recordModel; }
    public UUID getRecordId() { return recordId; }
    public UUID getSaleLineId() { return saleLineId; }
}
