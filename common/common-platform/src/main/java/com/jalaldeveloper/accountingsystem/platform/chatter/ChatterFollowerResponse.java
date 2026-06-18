package com.jalaldeveloper.accountingsystem.platform.chatter;

import java.time.Instant;
import java.util.UUID;

public class ChatterFollowerResponse {

    private final UUID id;
    private final UUID companyId;
    private final String modelName;
    private final UUID recordId;
    private final UUID partnerId;
    private final String addedBy;
    private final boolean notifyOnPost;
    private final Instant createdAt;

    public ChatterFollowerResponse(UUID id, UUID companyId, String modelName, UUID recordId,
                                   UUID partnerId, String addedBy, boolean notifyOnPost, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.modelName = modelName;
        this.recordId = recordId;
        this.partnerId = partnerId;
        this.addedBy = addedBy;
        this.notifyOnPost = notifyOnPost;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getModelName() { return modelName; }
    public UUID getRecordId() { return recordId; }
    public UUID getPartnerId() { return partnerId; }
    public String getAddedBy() { return addedBy; }
    public boolean isNotifyOnPost() { return notifyOnPost; }
    public Instant getCreatedAt() { return createdAt; }
}
