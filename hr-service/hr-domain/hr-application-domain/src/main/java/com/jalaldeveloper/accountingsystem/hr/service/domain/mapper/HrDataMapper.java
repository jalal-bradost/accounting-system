package com.jalaldeveloper.accountingsystem.hr.service.domain.mapper;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Department;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.Employee;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.DepartmentId;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.EmployeeId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HrDataMapper {

    public Employee createCommandToEmployee(CreateEmployeeCommand cmd, UUID id, CompanyId companyId) {
        return Employee.builder()
                .id(new EmployeeId(id))
                .companyId(companyId)
                .displayName(cmd.getDisplayName())
                .workEmail(cmd.getWorkEmail())
                .workPhone(cmd.getWorkPhone())
                .mobilePhone(cmd.getMobilePhone())
                .jobTitle(cmd.getJobTitle())
                .departmentId(cmd.getDepartmentId() != null ? new DepartmentId(cmd.getDepartmentId()) : null)
                .managerId(cmd.getManagerId() != null ? new EmployeeId(cmd.getManagerId()) : null)
                .linkedUserId(cmd.getUserId() != null ? new UserId(cmd.getUserId()) : null)
                .hireDate(cmd.getHireDate())
                .workStreet(cmd.getWorkStreet())
                .workCity(cmd.getWorkCity())
                .workState(cmd.getWorkState())
                .workPostalCode(cmd.getWorkPostalCode())
                .workCountry(cmd.getWorkCountry())
                .workLocation(cmd.getWorkLocation())
                .build();
    }

    public Department createCommandToDepartment(CreateDepartmentCommand cmd, UUID id, CompanyId companyId) {
        return Department.builder()
                .id(new DepartmentId(id))
                .companyId(companyId)
                .name(cmd.getName())
                .parentId(cmd.getParentId() != null ? new DepartmentId(cmd.getParentId()) : null)
                .managerId(cmd.getManagerId() != null ? new EmployeeId(cmd.getManagerId()) : null)
                .colorIndex(cmd.getColorIndex())
                .build();
    }

    public EmployeeResponse employeeToResponse(Employee e,
                                               String departmentName,
                                               String managerName,
                                               String managerImageUrl,
                                               String imageUrl,
                                               String imageContentType,
                                               UUID userId,
                                               String userDisplayName,
                                               String userEmail) {
        if (e == null) return null;
        return new EmployeeResponse(
                e.getId().getId(),
                e.getCompanyId().getId(),
                e.getDisplayName(),
                e.getWorkEmail(),
                e.getWorkPhone(),
                e.getMobilePhone(),
                e.getJobTitle(),
                e.getDepartmentId() != null ? e.getDepartmentId().getId() : null,
                departmentName,
                e.getManagerId() != null ? e.getManagerId().getId() : null,
                managerName,
                managerImageUrl,
                userId,
                userDisplayName,
                userEmail,
                e.getHireDate(),
                e.getWorkStreet(),
                e.getWorkCity(),
                e.getWorkState(),
                e.getWorkPostalCode(),
                e.getWorkCountry(),
                e.getWorkLocation(),
                imageUrl,
                imageContentType,
                e.isActive(),
                e.getArchivedAt(),
                e.getArchivedBy());
    }

    public EmployeeSummaryResponse employeeToSummary(Employee e,
                                                     String departmentName,
                                                     String managerName,
                                                     String managerImageUrl,
                                                     String imageUrl) {
        if (e == null) return null;
        return new EmployeeSummaryResponse(
                e.getId().getId(),
                e.getCompanyId().getId(),
                e.getDisplayName(),
                e.getWorkEmail(),
                e.getWorkPhone(),
                e.getJobTitle(),
                e.getDepartmentId() != null ? e.getDepartmentId().getId() : null,
                departmentName,
                e.getManagerId() != null ? e.getManagerId().getId() : null,
                managerName,
                managerImageUrl,
                imageUrl,
                e.getHireDate(),
                e.isActive());
    }

    public DepartmentResponse departmentToResponse(Department d, String managerName, String managerImageUrl) {
        if (d == null) return null;
        return new DepartmentResponse(
                d.getId().getId(),
                d.getCompanyId().getId(),
                d.getName(),
                d.getParentId() != null ? d.getParentId().getId() : null,
                d.getManagerId() != null ? d.getManagerId().getId() : null,
                managerName,
                managerImageUrl,
                d.getColorIndex(),
                d.isActive(),
                d.getArchivedAt(),
                d.getArchivedBy());
    }

    public DepartmentSummaryResponse departmentToSummary(Department d,
                                                         String managerName,
                                                         String managerImageUrl,
                                                         long employeeCount) {
        if (d == null) return null;
        return new DepartmentSummaryResponse(
                d.getId().getId(),
                d.getName(),
                d.getParentId() != null ? d.getParentId().getId() : null,
                d.getManagerId() != null ? d.getManagerId().getId() : null,
                managerName,
                managerImageUrl,
                d.getColorIndex(),
                employeeCount,
                d.isActive());
    }
}
