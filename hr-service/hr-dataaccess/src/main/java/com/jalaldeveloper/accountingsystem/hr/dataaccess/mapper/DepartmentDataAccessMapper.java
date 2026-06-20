package com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.DepartmentEntity;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Department;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import org.springframework.stereotype.Component;

@Component
public class DepartmentDataAccessMapper {

    public Department entityToDomain(DepartmentEntity e) {
        if (e == null) return null;
        Department.Builder b = Department.builder()
                .id(new DepartmentId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .name(e.getName())
                .parentId(e.getParentId() != null ? new DepartmentId(e.getParentId()) : null)
                .managerId(e.getManagerId() != null ? new EmployeeId(e.getManagerId()) : null)
                .colorIndex(e.getColorIndex());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public DepartmentEntity domainToEntity(Department d, DepartmentEntity existingOrNull) {
        if (d == null) return null;
        DepartmentEntity e = existingOrNull != null ? existingOrNull : new DepartmentEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setName(d.getName());
        e.setParentId(d.getParentId() != null ? d.getParentId().getId() : null);
        e.setManagerId(d.getManagerId() != null ? d.getManagerId().getId() : null);
        e.setColorIndex(d.getColorIndex());
        e.setActive(d.isActive());
        e.setArchivedAt(d.getArchivedAt());
        e.setArchivedBy(d.getArchivedBy());
        return e;
    }
}
