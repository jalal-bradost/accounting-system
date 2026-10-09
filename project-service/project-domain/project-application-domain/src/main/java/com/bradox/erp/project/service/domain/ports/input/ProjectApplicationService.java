package com.bradox.erp.project.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.service.domain.dto.MoveTaskCommand;
import com.bradox.erp.project.service.domain.dto.ProjectCommand;
import com.bradox.erp.project.service.domain.dto.ProjectResponse;
import com.bradox.erp.project.service.domain.dto.StageCommand;
import com.bradox.erp.project.service.domain.dto.StageResponse;
import com.bradox.erp.project.service.domain.dto.TaskCommand;
import com.bradox.erp.project.service.domain.dto.TaskResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectApplicationService {

    List<ProjectResponse> listProjects(CompanyId companyId);

    ProjectResponse getProject(CompanyId companyId, UUID id);

    /** Creates the project with the default stages New, In Progress and Done. */
    ProjectResponse createProject(CompanyId companyId, ProjectCommand command);

    ProjectResponse updateProject(CompanyId companyId, UUID id, ProjectCommand command);

    /** Deletes the project with its stages and tasks. */
    void deleteProject(CompanyId companyId, UUID id);

    List<StageResponse> listStages(CompanyId companyId, UUID projectId);

    StageResponse createStage(CompanyId companyId, UUID projectId, StageCommand command);

    StageResponse renameStage(CompanyId companyId, UUID stageId, StageCommand command);

    /** Moves a stage to {@code index} among the project's stages. */
    List<StageResponse> moveStage(CompanyId companyId, UUID stageId, int index);

    /** Refused while the stage still has tasks. */
    void deleteStage(CompanyId companyId, UUID stageId);

    List<TaskResponse> listTasks(CompanyId companyId, UUID projectId);

    TaskResponse getTask(CompanyId companyId, UUID id);

    TaskResponse createTask(CompanyId companyId, UUID projectId, TaskCommand command);

    TaskResponse updateTask(CompanyId companyId, UUID id, TaskCommand command);

    TaskResponse moveTask(CompanyId companyId, UUID id, MoveTaskCommand command);

    void deleteTask(CompanyId companyId, UUID id);
}
