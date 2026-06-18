package com.jalaldeveloper.accountingsystem.platform.chatter;

import java.time.Instant;
import java.util.UUID;

public class ChatterAttachmentResponse {

    private final UUID id;
    private final UUID companyId;
    private final String modelName;
    private final UUID recordId;
    private final String fileName;
    private final String contentType;
    private final long fileSize;
    private final String publicUrl;
    private final String uploadedBy;
    private final Instant createdAt;

    public ChatterAttachmentResponse(UUID id, UUID companyId, String modelName, UUID recordId,
                                     String fileName, String contentType, long fileSize,
                                     String publicUrl, String uploadedBy, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.modelName = modelName;
        this.recordId = recordId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.publicUrl = publicUrl;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getModelName() { return modelName; }
    public UUID getRecordId() { return recordId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public String getPublicUrl() { return publicUrl; }
    public String getUploadedBy() { return uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
