package com.bradox.erp.documents.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doc_folder")
public class FolderEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 255)
    private String nameNormalized;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "inherit_access", nullable = false)
    private boolean inheritAccess = true;

    @Column(name = "linked_model", length = 100)
    private String linkedModel;

    @Column(name = "system_key", length = 50)
    private String systemKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    public FolderEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String nameNormalized) { this.nameNormalized = nameNormalized; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public boolean isInheritAccess() { return inheritAccess; }
    public void setInheritAccess(boolean inheritAccess) { this.inheritAccess = inheritAccess; }
    public String getLinkedModel() { return linkedModel; }
    public void setLinkedModel(String linkedModel) { this.linkedModel = linkedModel; }
    public String getSystemKey() { return systemKey; }
    public void setSystemKey(String systemKey) { this.systemKey = systemKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
