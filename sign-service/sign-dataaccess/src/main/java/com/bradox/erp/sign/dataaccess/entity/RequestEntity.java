package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sign_request")
public class RequestEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "reference", nullable = false, length = 50)
    private String reference;
    @Column(name = "template_id")
    private UUID templateId;
    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;
    @Column(name = "source_version_id")
    private UUID sourceVersionId;
    @Column(name = "source_sha256", nullable = false, length = 64)
    private String sourceSha256;
    @Column(name = "page_count", nullable = false)
    private int pageCount;
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "signing_order", nullable = false, length = 16)
    private String signingOrder;
    @Column(name = "message", length = 2000)
    private String message;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "reminder_every_days")
    private Integer reminderEveryDays;
    @Column(name = "reminder_prompts", nullable = false)
    private int reminderPrompts;
    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;
    @Column(name = "record_model", length = 100)
    private String recordModel;
    @Column(name = "record_id")
    private UUID recordId;
    @Column(name = "final_document_id")
    private UUID finalDocumentId;
    @Column(name = "final_sha256", length = 64)
    private String finalSha256;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "canceled_at")
    private Instant canceledAt;
    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;
    @Column(name = "needs_attention", nullable = false)
    private boolean needsAttention;
    @Column(name = "attention_reason", length = 1000)
    private String attentionReason;
    @Column(name = "build_attempts", nullable = false)
    private int buildAttempts;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 255)
    private String createdBy;
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    public RequestEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(UUID sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }
    public UUID getSourceVersionId() { return sourceVersionId; }
    public void setSourceVersionId(UUID sourceVersionId) { this.sourceVersionId = sourceVersionId; }
    public String getSourceSha256() { return sourceSha256; }
    public void setSourceSha256(String sourceSha256) { this.sourceSha256 = sourceSha256; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSigningOrder() { return signingOrder; }
    public void setSigningOrder(String signingOrder) { this.signingOrder = signingOrder; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Integer getReminderEveryDays() { return reminderEveryDays; }
    public void setReminderEveryDays(Integer reminderEveryDays) { this.reminderEveryDays = reminderEveryDays; }
    public int getReminderPrompts() { return reminderPrompts; }
    public void setReminderPrompts(int reminderPrompts) { this.reminderPrompts = reminderPrompts; }
    public Instant getLastReminderAt() { return lastReminderAt; }
    public void setLastReminderAt(Instant lastReminderAt) { this.lastReminderAt = lastReminderAt; }
    public String getRecordModel() { return recordModel; }
    public void setRecordModel(String recordModel) { this.recordModel = recordModel; }
    public UUID getRecordId() { return recordId; }
    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public UUID getFinalDocumentId() { return finalDocumentId; }
    public void setFinalDocumentId(UUID finalDocumentId) { this.finalDocumentId = finalDocumentId; }
    public String getFinalSha256() { return finalSha256; }
    public void setFinalSha256(String finalSha256) { this.finalSha256 = finalSha256; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public void setCanceledAt(Instant canceledAt) { this.canceledAt = canceledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public boolean isNeedsAttention() { return needsAttention; }
    public void setNeedsAttention(boolean needsAttention) { this.needsAttention = needsAttention; }
    public String getAttentionReason() { return attentionReason; }
    public void setAttentionReason(String attentionReason) { this.attentionReason = attentionReason; }
    public int getBuildAttempts() { return buildAttempts; }
    public void setBuildAttempts(int buildAttempts) { this.buildAttempts = buildAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
}
