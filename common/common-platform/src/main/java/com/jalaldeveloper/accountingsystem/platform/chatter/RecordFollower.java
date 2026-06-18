package com.jalaldeveloper.accountingsystem.platform.chatter;

import java.time.Instant;
import java.util.UUID;

public class RecordFollower {

    private UUID id;
    private UUID companyId;
    private String modelName;
    private UUID recordId;
    private UUID partnerId;
    private String addedBy;
    private boolean notifyOnPost;
    private Instant createdAt;

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
