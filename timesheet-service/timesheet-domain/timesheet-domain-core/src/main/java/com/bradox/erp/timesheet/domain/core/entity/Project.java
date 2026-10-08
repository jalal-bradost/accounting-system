package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.exception.TimesheetDomainException;
import com.bradox.erp.timesheet.domain.core.valueobject.BillingMode;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** A light project (D1): the place time is logged against. No stages, no sub-tasks. */
public class Project extends AggregateRoot<ProjectId> {

    public static final String INTERNAL_KEY = "internal";

    private CompanyId companyId;
    private String name;
    private String code;
    private UUID partnerId;
    private UUID managerEmployeeId;
    private boolean allowTimesheets;
    private BillingMode billingMode;
    private boolean billableDefault;
    private Integer allocatedMinutes;
    private String linkedModel;
    private String systemKey;
    private ProjectStatus status;
    private String color;
    private UUID defaultSaleLineId;
    private UUID costAccountId;
    private Instant createdAt;
    private String createdBy;

    private Project() {
    }

    public static Project create(ProjectId id, CompanyId companyId, String name, String code, UUID partnerId,
                                 UUID managerEmployeeId, boolean allowTimesheets, BillingMode billingMode,
                                 boolean billableDefault, Integer allocatedMinutes, String color,
                                 String systemKey, Instant now, String createdBy) {
        if (companyId == null) {
            throw new TimesheetDomainException("error.timesheet.companyRequired", null, "companyId required");
        }
        Project p = new Project();
        p.setId(id);
        p.companyId = companyId;
        p.systemKey = systemKey;
        p.status = ProjectStatus.ACTIVE;
        p.createdAt = now;
        p.createdBy = createdBy;
        p.applyDetails(name, code, partnerId, managerEmployeeId, allowTimesheets, billingMode, billableDefault,
                allocatedMinutes, color);
        return p;
    }

    /** A project fed by another module (TSH-09); its tasks are created by that module, not by hand. */
    public static Project createLinked(ProjectId id, CompanyId companyId, String name, String linkedModel,
                                       String systemKey, Instant now) {
        Project p = create(id, companyId, name, null, null, null, true, BillingMode.HOURLY, false, null, null,
                systemKey, now, "system");
        p.linkedModel = linkedModel;
        return p;
    }

    public static Project restore(ProjectId id, CompanyId companyId, String name, String code, UUID partnerId,
                                  UUID managerEmployeeId, boolean allowTimesheets, BillingMode billingMode,
                                  boolean billableDefault, Integer allocatedMinutes, String linkedModel,
                                  String systemKey, ProjectStatus status, String color, Instant createdAt,
                                  String createdBy) {
        return restore(id, companyId, name, code, partnerId, managerEmployeeId, allowTimesheets, billingMode,
                billableDefault, allocatedMinutes, linkedModel, systemKey, status, color, createdAt, createdBy, null, null);
    }

    public static Project restore(ProjectId id, CompanyId companyId, String name, String code, UUID partnerId,
                                  UUID managerEmployeeId, boolean allowTimesheets, BillingMode billingMode,
                                  boolean billableDefault, Integer allocatedMinutes, String linkedModel,
                                  String systemKey, ProjectStatus status, String color, Instant createdAt,
                                  String createdBy, UUID defaultSaleLineId, UUID costAccountId) {
        Project p = new Project();
        p.defaultSaleLineId = defaultSaleLineId;
        p.costAccountId = costAccountId;
        p.setId(id);
        p.companyId = companyId;
        p.name = name;
        p.code = code;
        p.partnerId = partnerId;
        p.managerEmployeeId = managerEmployeeId;
        p.allowTimesheets = allowTimesheets;
        p.billingMode = billingMode;
        p.billableDefault = billableDefault;
        p.allocatedMinutes = allocatedMinutes;
        p.linkedModel = linkedModel;
        p.systemKey = systemKey;
        p.status = status;
        p.color = color;
        p.createdAt = createdAt;
        p.createdBy = createdBy;
        return p;
    }

    public void update(String name, String code, UUID partnerId, UUID managerEmployeeId, boolean allowTimesheets,
                       BillingMode billingMode, boolean billableDefault, Integer allocatedMinutes, String color) {
        if (isSystemManaged() && billingMode != this.billingMode) {
            throw new TimesheetDomainException("error.timesheet.projectSystemManaged", null,
                    "This project is managed by the system and its billing mode cannot change");
        }
        applyDetails(name, code, partnerId, managerEmployeeId, allowTimesheets, billingMode, billableDefault,
                allocatedMinutes, color);
    }

    private void applyDetails(String name, String code, UUID partnerId, UUID managerEmployeeId,
                              boolean allowTimesheets, BillingMode billingMode, boolean billableDefault,
                              Integer allocatedMinutes, String color) {
        if (name == null || name.isBlank()) {
            throw new TimesheetDomainException("error.timesheet.projectNameRequired", null, "Project name is required");
        }
        if (allocatedMinutes != null && allocatedMinutes < 0) {
            throw new TimesheetDomainException("error.timesheet.allocatedInvalid", null,
                    "Allocated hours cannot be negative");
        }
        this.name = name.trim();
        this.code = code == null || code.isBlank() ? null : code.trim();
        this.partnerId = partnerId;
        this.managerEmployeeId = managerEmployeeId;
        this.allowTimesheets = allowTimesheets;
        this.billingMode = billingMode == null ? BillingMode.HOURLY : billingMode;
        // Fixed-price jobs: hours are cost only, so "billable by default" is off and locked (TSH-01 #1).
        this.billableDefault = this.billingMode == BillingMode.FIXED_PRICE ? false : billableDefault;
        this.allocatedMinutes = allocatedMinutes;
        this.color = color == null || color.isBlank() ? "#64748b" : color.trim();
    }

    /** TSH-06 and TSH-10 settings of a project. A fixed-price project never bills hours, so it keeps no order line. */
    public void configureAccounting(UUID defaultSaleLineId, UUID costAccountId) {
        this.defaultSaleLineId = billingMode == BillingMode.FIXED_PRICE ? null : defaultSaleLineId;
        this.costAccountId = costAccountId;
    }

    public void archive() {
        if (INTERNAL_KEY.equals(systemKey)) {
            throw new TimesheetDomainException("error.timesheet.internalProjectArchive", null,
                    "The Internal project cannot be archived");
        }
        this.status = ProjectStatus.ARCHIVED;
    }

    public void unarchive() {
        this.status = ProjectStatus.ACTIVE;
    }

    /** BR-TSH-04: new entries need an active project that allows timesheets. */
    public boolean acceptsEntries() {
        return status == ProjectStatus.ACTIVE && allowTimesheets;
    }

    /** TSH-01 #7: projects fed by another module are system-managed. */
    public boolean isSystemManaged() {
        return linkedModel != null;
    }

    public static String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public UUID getPartnerId() { return partnerId; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public boolean isAllowTimesheets() { return allowTimesheets; }
    public BillingMode getBillingMode() { return billingMode; }
    public boolean isBillableDefault() { return billableDefault; }
    public Integer getAllocatedMinutes() { return allocatedMinutes; }
    public String getLinkedModel() { return linkedModel; }
    public String getSystemKey() { return systemKey; }
    public ProjectStatus getStatus() { return status; }
    public String getColor() { return color; }
    public UUID getDefaultSaleLineId() { return defaultSaleLineId; }
    public UUID getCostAccountId() { return costAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
