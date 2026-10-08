package com.bradox.erp.timesheet.domain.core.entity;

import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.TaskId;
import com.bradox.erp.timesheet.domain.core.valueobject.TimerId;

import java.time.Instant;
import java.util.UUID;

/** A running timer (BR-TSH-08: one per employee). Kept on the server so it survives closing the browser. */
public class Timer extends AggregateRoot<TimerId> {

    private CompanyId companyId;
    private UUID employeeId;
    private ProjectId projectId;
    private TaskId taskId;
    private String description;
    private Instant startedAt;

    private Timer() {
    }

    public static Timer start(TimerId id, CompanyId companyId, UUID employeeId, ProjectId projectId, TaskId taskId,
                              String description, Instant now) {
        Timer t = new Timer();
        t.setId(id);
        t.companyId = companyId;
        t.employeeId = employeeId;
        t.projectId = projectId;
        t.taskId = taskId;
        t.description = description == null || description.isBlank() ? null : description.trim();
        t.startedAt = now;
        return t;
    }

    public static Timer restore(TimerId id, CompanyId companyId, UUID employeeId, ProjectId projectId, TaskId taskId,
                                String description, Instant startedAt) {
        Timer t = new Timer();
        t.setId(id);
        t.companyId = companyId;
        t.employeeId = employeeId;
        t.projectId = projectId;
        t.taskId = taskId;
        t.description = description;
        t.startedAt = startedAt;
        return t;
    }

    public CompanyId getCompanyId() { return companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public ProjectId getProjectId() { return projectId; }
    public TaskId getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public Instant getStartedAt() { return startedAt; }
}
