package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tsh_project")
public class ProjectEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(length = 50)
    private String code;
    @Column(name = "code_normalized", length = 50)
    private String codeNormalized;
    @Column(name = "partner_id")
    private UUID partnerId;
    @Column(name = "manager_employee_id")
    private UUID managerEmployeeId;
    @Column(name = "allow_timesheets", nullable = false)
    private boolean allowTimesheets;
    @Column(name = "billing_mode", nullable = false, length = 16)
    private String billingMode;
    @Column(name = "billable_default", nullable = false)
    private boolean billableDefault;
    @Column(name = "allocated_minutes")
    private Integer allocatedMinutes;
    @Column(name = "linked_model", length = 100)
    private String linkedModel;
    @Column(name = "system_key", length = 50)
    private String systemKey;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(length = 20)
    private String color;
    @Column(name = "default_sale_line_id")
    private UUID defaultSaleLineId;
    @Column(name = "cost_account_id")
    private UUID costAccountId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 255)
    private String createdBy;

    public ProjectEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeNormalized() { return codeNormalized; }
    public void setCodeNormalized(String codeNormalized) { this.codeNormalized = codeNormalized; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(UUID managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }
    public boolean isAllowTimesheets() { return allowTimesheets; }
    public void setAllowTimesheets(boolean allowTimesheets) { this.allowTimesheets = allowTimesheets; }
    public String getBillingMode() { return billingMode; }
    public void setBillingMode(String billingMode) { this.billingMode = billingMode; }
    public boolean isBillableDefault() { return billableDefault; }
    public void setBillableDefault(boolean billableDefault) { this.billableDefault = billableDefault; }
    public Integer getAllocatedMinutes() { return allocatedMinutes; }
    public void setAllocatedMinutes(Integer allocatedMinutes) { this.allocatedMinutes = allocatedMinutes; }
    public String getLinkedModel() { return linkedModel; }
    public void setLinkedModel(String linkedModel) { this.linkedModel = linkedModel; }
    public String getSystemKey() { return systemKey; }
    public void setSystemKey(String systemKey) { this.systemKey = systemKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getDefaultSaleLineId() { return defaultSaleLineId; }
    public void setDefaultSaleLineId(UUID v) { this.defaultSaleLineId = v; }
    public UUID getCostAccountId() { return costAccountId; }
    public void setCostAccountId(UUID v) { this.costAccountId = v; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
