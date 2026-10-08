package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Reference from a document to a business record, for example an invoice or an employee. */
public final class RecordLink {

    private final String modelName;
    private final UUID recordId;
    private final Instant createdAt;
    private final String createdBy;

    public RecordLink(String modelName, UUID recordId, Instant createdAt, String createdBy) {
        if (modelName == null || modelName.isBlank() || recordId == null) {
            throw new DocumentsDomainException("error.documents.linkRequired", null, "Model and record are required");
        }
        this.modelName = modelName.trim();
        this.recordId = recordId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getModelName() { return modelName; }
    public UUID getRecordId() { return recordId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecordLink other)) return false;
        return modelName.equals(other.modelName) && recordId.equals(other.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, recordId);
    }
}
