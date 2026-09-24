package com.bradox.erp.hr.domain.core.entity;

import com.bradox.erp.domain.entity.ArchivableAggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.hr.domain.core.exception.HrDomainException;
import com.bradox.erp.hr.domain.core.valueobject.DepartmentId;
import com.bradox.erp.hr.domain.core.valueobject.EmployeeId;

import java.time.Instant;

public class Department extends ArchivableAggregateRoot<DepartmentId> {

    private final CompanyId companyId;
    private final String name;
    private final DepartmentId parentId;
    private final EmployeeId managerId;
    private final int colorIndex;

    private Department(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.name = b.name;
        this.parentId = b.parentId;
        this.managerId = b.managerId;
        this.colorIndex = b.colorIndex;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new HrDomainException("error.hr.companyIdRequired", null, "companyId required");
        if (name == null || name.isBlank()) throw new HrDomainException("error.hr.nameRequired", null, "name required");
        if (colorIndex < 0) throw new HrDomainException("error.hr.colorIndexNonNegative", null, "colorIndex must be >= 0");
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public DepartmentId getParentId() { return parentId; }
    public EmployeeId getManagerId() { return managerId; }
    public int getColorIndex() { return colorIndex; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private DepartmentId id;
        private CompanyId companyId;
        private String name;
        private DepartmentId parentId;
        private EmployeeId managerId;
        private int colorIndex;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(DepartmentId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder parentId(DepartmentId v) { this.parentId = v; return this; }
        public Builder managerId(EmployeeId v) { this.managerId = v; return this; }
        public Builder colorIndex(int v) { this.colorIndex = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public Department build() { return new Department(this); }
    }
}
