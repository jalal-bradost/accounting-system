package com.jalaldeveloper.accountingsystem.hr.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;

import java.time.Instant;
import java.time.LocalDate;

public class Employee extends ArchivableAggregateRoot<EmployeeId> {

    private final CompanyId companyId;
    private final String displayName;
    private final String workEmail;
    private final String workPhone;
    private final String mobilePhone;
    private final String jobTitle;
    private final DepartmentId departmentId;
    private final EmployeeId managerId;
    private final UserId linkedUserId;
    private final LocalDate hireDate;
    private final String workStreet;
    private final String workCity;
    private final String workState;
    private final String workPostalCode;
    private final String workCountry;
    private final String workLocation;

    private Employee(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.displayName = b.displayName;
        this.workEmail = b.workEmail;
        this.workPhone = b.workPhone;
        this.mobilePhone = b.mobilePhone;
        this.jobTitle = b.jobTitle;
        this.departmentId = b.departmentId;
        this.managerId = b.managerId;
        this.linkedUserId = b.linkedUserId;
        this.hireDate = b.hireDate;
        this.workStreet = b.workStreet;
        this.workCity = b.workCity;
        this.workState = b.workState;
        this.workPostalCode = b.workPostalCode;
        this.workCountry = b.workCountry;
        this.workLocation = b.workLocation;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("companyId required");
        if (displayName == null || displayName.isBlank()) throw new HrDomainException("displayName required");
    }

    public Employee linkUser(UserId userId) {
        if (userId == null) {
            throw new HrDomainException("userId required to link user");
        }
        return toBuilder().linkedUserId(userId).build();
    }

    public Employee unlinkUser() {
        return toBuilder().linkedUserId(null).build();
    }

    public Builder toBuilder() {
        Builder b = new Builder()
                .id(getId())
                .companyId(companyId)
                .displayName(displayName)
                .workEmail(workEmail)
                .workPhone(workPhone)
                .mobilePhone(mobilePhone)
                .jobTitle(jobTitle)
                .departmentId(departmentId)
                .managerId(managerId)
                .linkedUserId(linkedUserId)
                .hireDate(hireDate)
                .workStreet(workStreet)
                .workCity(workCity)
                .workState(workState)
                .workPostalCode(workPostalCode)
                .workCountry(workCountry)
                .workLocation(workLocation);
        if (!isActive()) {
            b.archived(true).archivedAt(getArchivedAt()).archivedBy(getArchivedBy());
        }
        return b;
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getDisplayName() { return displayName; }
    public String getWorkEmail() { return workEmail; }
    public String getWorkPhone() { return workPhone; }
    public String getMobilePhone() { return mobilePhone; }
    public String getJobTitle() { return jobTitle; }
    public DepartmentId getDepartmentId() { return departmentId; }
    public EmployeeId getManagerId() { return managerId; }
    public UserId getLinkedUserId() { return linkedUserId; }
    public LocalDate getHireDate() { return hireDate; }
    public String getWorkStreet() { return workStreet; }
    public String getWorkCity() { return workCity; }
    public String getWorkState() { return workState; }
    public String getWorkPostalCode() { return workPostalCode; }
    public String getWorkCountry() { return workCountry; }
    public String getWorkLocation() { return workLocation; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private EmployeeId id;
        private CompanyId companyId;
        private String displayName;
        private String workEmail;
        private String workPhone;
        private String mobilePhone;
        private String jobTitle;
        private DepartmentId departmentId;
        private EmployeeId managerId;
        private UserId linkedUserId;
        private LocalDate hireDate;
        private String workStreet;
        private String workCity;
        private String workState;
        private String workPostalCode;
        private String workCountry;
        private String workLocation;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(EmployeeId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder displayName(String v) { this.displayName = v; return this; }
        public Builder workEmail(String v) { this.workEmail = v; return this; }
        public Builder workPhone(String v) { this.workPhone = v; return this; }
        public Builder mobilePhone(String v) { this.mobilePhone = v; return this; }
        public Builder jobTitle(String v) { this.jobTitle = v; return this; }
        public Builder departmentId(DepartmentId v) { this.departmentId = v; return this; }
        public Builder managerId(EmployeeId v) { this.managerId = v; return this; }
        public Builder linkedUserId(UserId v) { this.linkedUserId = v; return this; }
        public Builder hireDate(LocalDate v) { this.hireDate = v; return this; }
        public Builder workStreet(String v) { this.workStreet = v; return this; }
        public Builder workCity(String v) { this.workCity = v; return this; }
        public Builder workState(String v) { this.workState = v; return this; }
        public Builder workPostalCode(String v) { this.workPostalCode = v; return this; }
        public Builder workCountry(String v) { this.workCountry = v; return this; }
        public Builder workLocation(String v) { this.workLocation = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public Employee build() { return new Employee(this); }
    }
}
