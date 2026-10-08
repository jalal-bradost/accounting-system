package com.bradox.erp.documents.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RecordLinkEmbeddable {

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    public RecordLinkEmbeddable() {
    }

    public RecordLinkEmbeddable(String modelName, UUID recordId, Instant createdAt, String createdBy) {
        this.modelName = modelName;
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
        if (!(o instanceof RecordLinkEmbeddable other)) return false;
        return Objects.equals(modelName, other.modelName) && Objects.equals(recordId, other.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, recordId);
    }
}
