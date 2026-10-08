package com.bradox.erp.timesheet.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.security.RequiresPermission;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.timesheet.service.domain.dto.TaskCommand;
import com.bradox.erp.timesheet.service.domain.dto.TaskResponse;
import com.bradox.erp.timesheet.service.domain.dto.TaskStatusCommand;
import com.bradox.erp.timesheet.service.domain.ports.input.TaskApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/timesheet/tasks", produces = "application/json")
public class TaskController {

    private final TaskApplicationService tasks;

    public TaskController(TaskApplicationService tasks) {
        this.tasks = tasks;
    }

    /** Tasks that can still take time, for the pickers. */
    @GetMapping("/open")
    @RequiresPermission("tsh.entry.own")
    public List<TaskResponse> open(@CurrentCompany CompanyId companyId) {
        return tasks.listOpen(companyId);
    }

    @PostMapping
    @RequiresPermission("tsh.task.manage")
    public TaskResponse create(@CurrentCompany CompanyId companyId, @Valid @RequestBody TaskCommand command) {
        return tasks.create(companyId, command);
    }

    @PutMapping("/{id}")
    @RequiresPermission("tsh.task.manage")
    public TaskResponse update(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                               @Valid @RequestBody TaskCommand command) {
        return tasks.update(companyId, id, command);
    }

    @PostMapping("/{id}/status")
    @RequiresPermission("tsh.task.manage")
    public TaskResponse status(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                               @Valid @RequestBody TaskStatusCommand command) {
        return tasks.changeStatus(companyId, id, command);
    }
}
