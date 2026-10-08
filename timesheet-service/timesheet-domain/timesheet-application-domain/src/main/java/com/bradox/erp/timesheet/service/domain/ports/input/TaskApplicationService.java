package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.TaskCommand;
import com.bradox.erp.timesheet.service.domain.dto.TaskResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskStatusCommand;

import java.util.List;
import java.util.UUID;

public interface TaskApplicationService {

    List<TaskResponse> listByProject(CompanyId companyId, UUID projectId);

    /** Tasks that can still take time, across active projects, for pickers. */
    List<TaskResponse> listOpen(CompanyId companyId);

    TaskResponse create(CompanyId companyId, TaskCommand command);

    TaskResponse update(CompanyId companyId, UUID id, TaskCommand command);

    TaskResponse changeStatus(CompanyId companyId, UUID id, TaskStatusCommand command);
}
