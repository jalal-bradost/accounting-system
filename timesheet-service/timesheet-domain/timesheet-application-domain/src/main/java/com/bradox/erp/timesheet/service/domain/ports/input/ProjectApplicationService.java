package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.ProjectCommand;
import com.bradox.erp.timesheet.service.domain.dto.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectApplicationService {

    /** Also makes sure the "Internal" project exists (TSH-01 #9). */
    List<ProjectResponse> list(CompanyId companyId, boolean includeArchived);

    ProjectResponse get(CompanyId companyId, UUID id);

    ProjectResponse create(CompanyId companyId, ProjectCommand command);

    ProjectResponse update(CompanyId companyId, UUID id, ProjectCommand command);

    ProjectResponse archive(CompanyId companyId, UUID id);

    ProjectResponse unarchive(CompanyId companyId, UUID id);
}
