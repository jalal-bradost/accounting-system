package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Employee to sales order line, per project, so people on one project can bill at different prices (TSH-06 #3). */
public interface ProjectRateRepository {

    /** employeeId to saleLineId. */
    Map<UUID, UUID> find(ProjectId projectId);

    Map<UUID, Map<UUID, UUID>> findForProjects(Collection<ProjectId> projectIds);

    /** Replaces the whole table of the project. */
    void replace(CompanyId companyId, ProjectId projectId, Map<UUID, UUID> employeeToLine);
}
