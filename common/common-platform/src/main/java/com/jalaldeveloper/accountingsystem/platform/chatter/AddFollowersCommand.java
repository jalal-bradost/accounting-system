package com.jalaldeveloper.accountingsystem.platform.chatter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class AddFollowersCommand {

    private UUID companyId;

    @NotBlank
    private String modelName;

    @NotNull
    private UUID recordId;

    @NotEmpty
    private List<UUID> partnerIds;

    private boolean notifyRecipients = true;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public List<UUID> getPartnerIds() { return partnerIds; }
    public void setPartnerIds(List<UUID> partnerIds) { this.partnerIds = partnerIds; }
    public boolean isNotifyRecipients() { return notifyRecipients; }
    public void setNotifyRecipients(boolean notifyRecipients) { this.notifyRecipients = notifyRecipients; }
}
