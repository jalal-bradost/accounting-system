package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tsh_task")
public class TaskEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(length = 4000)
    private String description;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "status_changed_at")
    private Instant statusChangedAt;
    @Column(name = "allocated_minutes")
    private Integer allocatedMinutes;
    private LocalDate deadline;
    @Column(name = "record_model", length = 100)
    private String recordModel;
    @Column(name = "record_id")
    private UUID recordId;
    @Column(name = "sale_line_id")
    private UUID saleLineId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tsh_task_assignee", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "employee_id", nullable = false)
    private Set<UUID> assigneeEmployeeIds = new LinkedHashSet<>();

    public TaskEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public void setStatusChangedAt(Instant statusChangedAt) { this.statusChangedAt = statusChangedAt; }
    public Integer getAllocatedMinutes() { return allocatedMinutes; }
    public void setAllocatedMinutes(Integer allocatedMinutes) { this.allocatedMinutes = allocatedMinutes; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getRecordModel() { return recordModel; }
    public void setRecordModel(String recordModel) { this.recordModel = recordModel; }
    public UUID getSaleLineId() { return saleLineId; }
    public void setSaleLineId(UUID saleLineId) { this.saleLineId = saleLineId; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Set<UUID> getAssigneeEmployeeIds() { return assigneeEmployeeIds; }
    public void setAssigneeEmployeeIds(Set<UUID> assigneeEmployeeIds) { this.assigneeEmployeeIds = assigneeEmployeeIds; }
}
