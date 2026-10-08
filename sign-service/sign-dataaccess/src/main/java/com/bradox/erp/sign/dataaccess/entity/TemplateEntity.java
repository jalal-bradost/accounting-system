package com.bradox.erp.sign.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sign_template")
public class TemplateEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Column(name = "category", length = 100)
    private String category;
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    @Column(name = "document_version_id")
    private UUID documentVersionId;
    @Column(name = "document_sha256", nullable = false, length = 64)
    private String documentSha256;
    @Column(name = "page_count", nullable = false)
    private int pageCount;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "default_validity_days", nullable = false)
    private int defaultValidityDays;
    @Column(name = "default_message", length = 2000)
    private String defaultMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 255)
    private String createdBy;

    public TemplateEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getDocumentVersionId() { return documentVersionId; }
    public void setDocumentVersionId(UUID documentVersionId) { this.documentVersionId = documentVersionId; }
    public String getDocumentSha256() { return documentSha256; }
    public void setDocumentSha256(String documentSha256) { this.documentSha256 = documentSha256; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDefaultValidityDays() { return defaultValidityDays; }
    public void setDefaultValidityDays(int defaultValidityDays) { this.defaultValidityDays = defaultValidityDays; }
    public String getDefaultMessage() { return defaultMessage; }
    public void setDefaultMessage(String defaultMessage) { this.defaultMessage = defaultMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
