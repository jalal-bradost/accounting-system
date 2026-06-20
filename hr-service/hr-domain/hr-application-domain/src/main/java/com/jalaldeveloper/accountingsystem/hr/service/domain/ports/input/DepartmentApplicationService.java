package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateDepartmentCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.DepartmentResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.DepartmentSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateDepartmentCommand;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface DepartmentApplicationService {

    DepartmentResponse create(@Valid CreateDepartmentCommand command);

    DepartmentResponse update(UUID id, @Valid UpdateDepartmentCommand command);

    DepartmentResponse get(UUID id);

    List<DepartmentSummaryResponse> list(CompanyId companyId, boolean includeArchived);

    DepartmentResponse archive(UUID id);

    DepartmentResponse unarchive(UUID id);

    long countEmployees(UUID departmentId);
}
