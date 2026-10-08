package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.ProjectCommand;
import com.bradox.erp.timesheet.service.domain.dto.ProjectResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.ProjectApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.input.TaskApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/timesheet/projects", produces = "application/json")
public class ProjectController {

    private final ProjectApplicationService projects;
    private final TaskApplicationService tasks;

    public ProjectController(ProjectApplicationService projects, TaskApplicationService tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    @GetMapping
    @RequiresPermission("tsh.entry.own")
    public List<ProjectResponse> list(@CurrentCompany CompanyId companyId,
                                      @RequestParam(defaultValue = "false") boolean includeArchived) {
        return projects.list(companyId, includeArchived);
    }

    @GetMapping("/{id}")
    @RequiresPermission("tsh.entry.own")
    public ProjectResponse get(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return projects.get(companyId, id);
    }

    @GetMapping("/{id}/tasks")
    @RequiresPermission("tsh.entry.own")
    public List<TaskResponse> tasks(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return tasks.listByProject(companyId, id);
    }

    @PostMapping
    @RequiresPermission("tsh.project.manage")
    public ProjectResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody ProjectCommand command) {
        return projects.create(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("tsh.project.manage")
    public ProjectResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                  @Valid @RequestBody ProjectCommand command) {
        return projects.update(companyId, id, command);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("tsh.project.manage")
    public ProjectResponse archive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return projects.archive(companyId, id);
    }

    @PostMapping("/{id}/unarchive")
    @RequiresPermission("tsh.project.manage")
    public ProjectResponse unarchive(@CurrentCompany CompanyId companyId, @PathVariable UUID id) {
        return projects.unarchive(companyId, id);
    }
}
