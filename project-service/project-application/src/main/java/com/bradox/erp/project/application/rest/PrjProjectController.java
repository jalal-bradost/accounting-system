package com.bradox.erp.project.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.project.service.domain.dto.MoveTaskCommand;
import com.bradox.erp.project.service.domain.dto.ProjectCommand;
import com.bradox.erp.project.service.domain.dto.ProjectResponse;
import com.bradox.erp.project.service.domain.dto.StageCommand;
import com.bradox.erp.project.service.domain.dto.StageResponse;
import com.bradox.erp.project.service.domain.dto.TaskCommand;
import com.bradox.erp.project.service.domain.dto.TaskResponse;
import com.bradox.erp.project.service.domain.ports.input.ProjectApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/project", produces = "application/json")
public class PrjProjectController {

    private final ProjectApplicationService service;

    public PrjProjectController(ProjectApplicationService service) {
        this.service = service;
    }

    @GetMapping("/projects")
    public List<ProjectResponse> projects(@CurrentCompany CompanyId companyId) {
        return service.listProjects(companyId);
    }

    @GetMapping("/projects/{id}")
    public ProjectResponse project(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.getProject(companyId, id);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@CurrentCompany CompanyId companyId, @Valid @RequestBody ProjectCommand c) {
        return service.createProject(companyId, c);
    }

    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody ProjectCommand c) {
        return service.updateProject(companyId, id, c);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.deleteProject(companyId, id);
    }

    @GetMapping("/projects/{id}/stages")
    public List<StageResponse> stages(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.listStages(companyId, id);
    }

    @PostMapping("/projects/{id}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public StageResponse createStage(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody StageCommand c) {
        return service.createStage(companyId, id, c);
    }

    @PutMapping("/stages/{id}")
    public StageResponse renameStage(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody StageCommand c) {
        return service.renameStage(companyId, id, c);
    }

    @PostMapping("/stages/{id}/move")
    public List<StageResponse> moveStage(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @RequestParam int index) {
        return service.moveStage(companyId, id, index);
    }

    @DeleteMapping("/stages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStage(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.deleteStage(companyId, id);
    }

    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> tasks(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.listTasks(companyId, id);
    }

    @PostMapping("/projects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody TaskCommand c) {
        return service.createTask(companyId, id, c);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse task(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return service.getTask(companyId, id);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody TaskCommand c) {
        return service.updateTask(companyId, id, c);
    }

    @PostMapping("/tasks/{id}/move")
    public TaskResponse moveTask(@CurrentCompany CompanyId companyId, @PathVariable UUID id, @Valid @RequestBody MoveTaskCommand c) {
        return service.moveTask(companyId, id, c);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        service.deleteTask(companyId, id);
    }
}
