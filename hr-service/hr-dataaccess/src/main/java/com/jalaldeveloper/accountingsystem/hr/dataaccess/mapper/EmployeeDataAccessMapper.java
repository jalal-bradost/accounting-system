package com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.EmployeeEntity;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDataAccessMapper {

    public Employee entityToDomain(EmployeeEntity e) {
        if (e == null) return null;
        Employee.Builder b = Employee.builder()
                .id(new EmployeeId(e.getId()))
                .companyId(new CompanyId(e.getCompanyId()))
                .displayName(e.getDisplayName())
                .workEmail(e.getWorkEmail())
                .workPhone(e.getWorkPhone())
                .mobilePhone(e.getMobilePhone())
                .jobTitle(e.getJobTitle())
                .departmentId(e.getDepartmentId() != null ? new DepartmentId(e.getDepartmentId()) : null)
                .managerId(e.getManagerId() != null ? new EmployeeId(e.getManagerId()) : null)
                .hireDate(e.getHireDate())
                .workStreet(e.getWorkStreet())
                .workCity(e.getWorkCity())
                .workState(e.getWorkState())
                .workPostalCode(e.getWorkPostalCode())
                .workCountry(e.getWorkCountry())
                .workLocation(e.getWorkLocation());
        if (!e.isActive()) {
            b.archived(true).archivedAt(e.getArchivedAt()).archivedBy(e.getArchivedBy());
        }
        return b.build();
    }

    public EmployeeEntity domainToEntity(Employee d, EmployeeEntity existingOrNull) {
        if (d == null) return null;
        EmployeeEntity e = existingOrNull != null ? existingOrNull : new EmployeeEntity();
        e.setId(d.getId().getId());
        e.setCompanyId(d.getCompanyId().getId());
        e.setDisplayName(d.getDisplayName());
        e.setWorkEmail(d.getWorkEmail());
        e.setWorkPhone(d.getWorkPhone());
        e.setMobilePhone(d.getMobilePhone());
        e.setJobTitle(d.getJobTitle());
        e.setDepartmentId(d.getDepartmentId() != null ? d.getDepartmentId().getId() : null);
        e.setManagerId(d.getManagerId() != null ? d.getManagerId().getId() : null);
        e.setHireDate(d.getHireDate());
        e.setWorkStreet(d.getWorkStreet());
        e.setWorkCity(d.getWorkCity());
        e.setWorkState(d.getWorkState());
        e.setWorkPostalCode(d.getWorkPostalCode());
        e.setWorkCountry(d.getWorkCountry());
        e.setWorkLocation(d.getWorkLocation());
        e.setActive(d.isActive());
        e.setArchivedAt(d.getArchivedAt());
        e.setArchivedBy(d.getArchivedBy());
        return e;
    }
}
