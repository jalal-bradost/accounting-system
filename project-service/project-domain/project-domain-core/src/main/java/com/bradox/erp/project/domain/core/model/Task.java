package com.bradox.erp.project.domain.core.model;

import com.bradox.erp.project.domain.core.exception.ProjectDomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A task of a project, in one of its stages. Priority is 0 to 3 stars; sequence orders the cards in a stage. */
public record Task(UUID id, UUID companyId, UUID projectId, UUID stageId, String name, String description,
                   UUID customerPartnerId, String assigneeUsername, LocalDate deadline, int priority, int sequence,
                   Instant createdAt) {

    public static final int MAX_PRIORITY = 3;

    public Task {
        if (name == null || name.isBlank()) {
            throw new ProjectDomainException("error.project.task.name", null, "A task needs a title");
        }
        if (stageId == null) {
            throw new ProjectDomainException("error.project.task.stage", null, "A task needs a stage");
        }
        name = name.trim();
        description = description == null || description.isBlank() ? null : description;
        assigneeUsername = assigneeUsername == null || assigneeUsername.isBlank() ? null : assigneeUsername.trim();
        if (priority < 0 || priority > MAX_PRIORITY) {
            throw new ProjectDomainException("error.project.task.priority", null, "Priority must be 0 to 3 stars");
        }
    }

    public Task edit(UUID stage, String newName, String newDescription, UUID customer, String assignee, LocalDate newDeadline,
                     int newPriority) {
        return new Task(id, companyId, projectId, stage, newName, newDescription, customer, assignee, newDeadline, newPriority,
                sequence, createdAt);
    }

    public Task moveTo(UUID stage, int newSequence) {
        return new Task(id, companyId, projectId, stage, name, description, customerPartnerId, assigneeUsername, deadline,
                priority, newSequence, createdAt);
    }
}
