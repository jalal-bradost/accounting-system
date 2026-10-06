package com.bradox.erp.platform.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_user_star", indexes = {
        @Index(name = "ix_user_star_user_model", columnList = "company_id,user_id,model_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_star", columnNames = {"company_id", "user_id", "model_name", "record_id"})
})
public class UserStarEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UserStarEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
