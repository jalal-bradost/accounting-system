package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_record_follower", indexes = {
        @Index(name = "ix_record_follower_target", columnList = "company_id,model_name,record_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_record_follower_partner",
                columnNames = {"company_id", "model_name", "record_id", "partner_id"})
})
public class RecordFollowerEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "added_by", length = 255)
    private String addedBy;

    @Column(name = "notify_on_post", nullable = false)
    private boolean notifyOnPost = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RecordFollowerEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public boolean isNotifyOnPost() { return notifyOnPost; }
    public void setNotifyOnPost(boolean notifyOnPost) { this.notifyOnPost = notifyOnPost; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
