package com.jalaldeveloper.accountingsystem.platform.activity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ActivityResponse {

    private final UUID id;
    private final UUID companyId;
    private final String modelName;
    private final UUID recordId;
    private final ActivityKind kind;
    private final String subject;
    private final String body;
    private final String authorId;
    private final String assigneeId;
    private final LocalDate dueDate;
    private final Instant completedAt;
    private final Instant createdAt;

    public ActivityResponse(UUID id, UUID companyId, String modelName, UUID recordId,
                            ActivityKind kind, String subject, String body,
                            String authorId, String assigneeId, LocalDate dueDate,
                            Instant completedAt, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.modelName = modelName;
        this.recordId = recordId;
        this.kind = kind;
        this.subject = subject;
        this.body = body;
        this.authorId = authorId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getModelName() { return modelName; }
    public UUID getRecordId() { return recordId; }
    public ActivityKind getKind() { return kind; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getAuthorId() { return authorId; }
    public String getAssigneeId() { return assigneeId; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
