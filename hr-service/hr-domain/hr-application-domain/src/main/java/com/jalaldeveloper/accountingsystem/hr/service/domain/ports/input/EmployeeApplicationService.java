package com.jalaldeveloper.accountingsystem.hr.service.domain.ports.input;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.CreateEmployeeCommand;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.EmployeeSummaryResponse;
import com.jalaldeveloper.accountingsystem.hr.service.domain.dto.UpdateEmployeeCommand;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface EmployeeApplicationService {

    EmployeeResponse create(@Valid CreateEmployeeCommand command);

    EmployeeResponse update(UUID id, @Valid UpdateEmployeeCommand command);

    EmployeeResponse get(UUID id);

    Page<EmployeeSummaryResponse> search(CompanyId companyId, String q, UUID departmentId,
                                         boolean includeArchived, Pageable pageable);

    EmployeeResponse archive(UUID id);

    EmployeeResponse unarchive(UUID id);

    EmployeeResponse uploadImage(UUID id, MultipartFile file);

    EmployeeResponse deleteImage(UUID id);
}
