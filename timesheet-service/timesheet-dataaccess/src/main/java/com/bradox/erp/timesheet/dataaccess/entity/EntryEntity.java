package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tsh_entry")
public class EntryEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(nullable = false)
    private int minutes;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "task_id")
    private UUID taskId;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private boolean billable;
    @Column(name = "week_id", nullable = false)
    private UUID weekId;
    @Column(nullable = false, length = 16)
    private String source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 255)
    private String createdBy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "cost_rate", precision = 19, scale = 4)
    private BigDecimal costRate;
    @Column(name = "cost_currency", length = 8)
    private String costCurrency;
    @Column(name = "cost_amount", precision = 19, scale = 4)
    private BigDecimal costAmount;
    @Column(name = "cost_missing", nullable = false)
    private boolean costMissing;
    @Column(name = "record_model", length = 100)
    private String recordModel;
    @Column(name = "record_id")
    private UUID recordId;
    @Column(name = "sale_line_id")
    private UUID saleLineId;

    public EntryEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isBillable() { return billable; }
    public void setBillable(boolean billable) { this.billable = billable; }
    public UUID getWeekId() { return weekId; }
    public void setWeekId(UUID weekId) { this.weekId = weekId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public BigDecimal getCostRate() { return costRate; }
    public void setCostRate(BigDecimal costRate) { this.costRate = costRate; }
    public String getCostCurrency() { return costCurrency; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public boolean isCostMissing() { return costMissing; }
    public void setCostMissing(boolean costMissing) { this.costMissing = costMissing; }
    public String getRecordModel() { return recordModel; }
    public void setRecordModel(String recordModel) { this.recordModel = recordModel; }
    public UUID getSaleLineId() { return saleLineId; }
    public void setSaleLineId(UUID saleLineId) { this.saleLineId = saleLineId; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
