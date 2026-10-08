package com.bradox.erp.documents.dataaccess.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "doc_document")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 255)
    private String nameNormalized;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_name_normalized", length = 255)
    private String fileNameNormalized;

    @Column(name = "content_type", length = 127)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String sha256;

    @Column(name = "trashed_at")
    private Instant trashedAt;

    @Column(name = "trashed_by", length = 255)
    private String trashedBy;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    @Column(name = "retention_reason", length = 500)
    private String retentionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @ElementCollection
    @CollectionTable(name = "doc_document_tag", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag_id", nullable = false)
    @BatchSize(size = 100)
    private Set<UUID> tagIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "doc_record_link", joinColumns = @JoinColumn(name = "document_id"))
    @BatchSize(size = 100)
    private Set<RecordLinkEmbeddable> links = new LinkedHashSet<>();

    public DocumentEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getFolderId() { return folderId; }
    public void setFolderId(UUID folderId) { this.folderId = folderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String nameNormalized) { this.nameNormalized = nameNormalized; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileNameNormalized() { return fileNameNormalized; }
    public void setFileNameNormalized(String fileNameNormalized) { this.fileNameNormalized = fileNameNormalized; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public Instant getTrashedAt() { return trashedAt; }
    public void setTrashedAt(Instant trashedAt) { this.trashedAt = trashedAt; }
    public String getTrashedBy() { return trashedBy; }
    public void setTrashedBy(String trashedBy) { this.trashedBy = trashedBy; }
    public LocalDate getRetentionUntil() { return retentionUntil; }
    public void setRetentionUntil(LocalDate retentionUntil) { this.retentionUntil = retentionUntil; }
    public String getRetentionReason() { return retentionReason; }
    public void setRetentionReason(String retentionReason) { this.retentionReason = retentionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Set<UUID> getTagIds() { return tagIds; }
    public void setTagIds(Set<UUID> tagIds) { this.tagIds = tagIds; }
    public Set<RecordLinkEmbeddable> getLinks() { return links; }
    public void setLinks(Set<RecordLinkEmbeddable> links) { this.links = links; }
}
