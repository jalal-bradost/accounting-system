package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Task extends AggregateRoot<TaskId> {

    private CompanyId companyId;
    private ProjectId projectId;
    private String name;
    private String description;
    private TaskStatus status;
    private Instant statusChangedAt;
    private Set<UUID> assigneeEmployeeIds = new LinkedHashSet<>();
    private Integer allocatedMinutes;
    private LocalDate deadline;
    private String recordModel;
    private UUID recordId;
    private UUID saleLineId;
    private Instant createdAt;
    private String createdBy;

    private Task() {
    }

    public static Task create(TaskId id, CompanyId companyId, ProjectId projectId, String name, String description,
                              Set<UUID> assignees, Integer allocatedMinutes, LocalDate deadline,
                              Instant now, String createdBy) {
        Task t = new Task();
        t.setId(id);
        t.companyId = companyId;
        t.projectId = projectId;
        t.status = TaskStatus.TODO;
        t.statusChangedAt = now;
        t.createdAt = now;
        t.createdBy = createdBy;
        t.applyDetails(name, description, assignees, allocatedMinutes, deadline);
        return t;
    }

    public static Task restore(TaskId id, CompanyId companyId, ProjectId projectId, String name, String description,
                               TaskStatus status, Instant statusChangedAt, Set<UUID> assignees,
                               Integer allocatedMinutes, LocalDate deadline, String recordModel, UUID recordId,
                               Instant createdAt, String createdBy) {
        return restore(id, companyId, projectId, name, description, status, statusChangedAt, assignees, allocatedMinutes,
                deadline, recordModel, recordId, createdAt, createdBy, null);
    }

    public static Task restore(TaskId id, CompanyId companyId, ProjectId projectId, String name, String description,
                               TaskStatus status, Instant statusChangedAt, Set<UUID> assignees,
                               Integer allocatedMinutes, LocalDate deadline, String recordModel, UUID recordId,
                               Instant createdAt, String createdBy, UUID saleLineId) {
        Task t = new Task();
        t.saleLineId = saleLineId;
        t.setId(id);
        t.companyId = companyId;
        t.projectId = projectId;
        t.name = name;
        t.description = description;
        t.status = status;
        t.statusChangedAt = statusChangedAt;
        t.assigneeEmployeeIds = assignees == null ? new LinkedHashSet<>() : new LinkedHashSet<>(assignees);
        t.allocatedMinutes = allocatedMinutes;
        t.deadline = deadline;
        t.recordModel = recordModel;
        t.recordId = recordId;
        t.createdAt = createdAt;
        t.createdBy = createdBy;
        return t;
    }

    public void update(String name, String description, Set<UUID> assignees, Integer allocatedMinutes,
                       LocalDate deadline) {
        applyDetails(name, description, assignees, allocatedMinutes, deadline);
    }

    private void applyDetails(String name, String description, Set<UUID> assignees, Integer allocatedMinutes,
                              LocalDate deadline) {
        if (name == null || name.isBlank()) {
            throw new TimesheetDomainException("error.timesheet.taskNameRequired", null, "Task name is required");
        }
        if (allocatedMinutes != null && allocatedMinutes < 0) {
            throw new TimesheetDomainException("error.timesheet.allocatedInvalid", null,
                    "Allocated hours cannot be negative");
        }
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.assigneeEmployeeIds = assignees == null ? new LinkedHashSet<>() : new LinkedHashSet<>(assignees);
        this.allocatedMinutes = allocatedMinutes;
        this.deadline = deadline;
    }

    /** Any move between different statuses is allowed; reopening a DONE task is a manager decision. */
    public void changeStatus(TaskStatus newStatus, Instant now) {
        if (newStatus == null) {
            throw new TimesheetDomainException("error.timesheet.taskStatusRequired", null, "Status is required");
        }
        if (newStatus != this.status) {
            this.status = newStatus;
            this.statusChangedAt = now;
        }
    }

    /** BR-TSH-04: DONE and CANCELED tasks take no new entries. */
    public boolean acceptsEntries() {
        return status == TaskStatus.TODO || status == TaskStatus.IN_PROGRESS;
    }

    /** TSH-09: tasks created for another module's record. */
    public void linkRecord(String model, UUID id) {
        this.recordModel = model;
        this.recordId = id;
    }

    /** TSH-06: the order line hours on this task bill to (first in the resolution order). */
    public void assignSaleLine(UUID saleLineId) {
        this.saleLineId = saleLineId;
    }

    public boolean isSystemManaged() {
        return recordModel != null;
    }

    public CompanyId getCompanyId() { return companyId; }
    public ProjectId getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public Set<UUID> getAssigneeEmployeeIds() { return Set.copyOf(assigneeEmployeeIds); }
    public Integer getAllocatedMinutes() { return allocatedMinutes; }
    public LocalDate getDeadline() { return deadline; }
    public UUID getSaleLineId() { return saleLineId; }
    public String getRecordModel() { return recordModel; }
    public UUID getRecordId() { return recordId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
