package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.FolderTreeRules;
import com.bradox.erp.documents.domain.core.rule.NameNormalizer;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.time.Instant;

public class Folder extends AggregateRoot<FolderId> {

    private CompanyId companyId;
    private FolderId parentId;
    private String name;
    private String nameNormalized;
    private int sequence;
    private boolean archived;
    private boolean inheritAccess;
    private String linkedModel;
    private String systemKey;
    private Instant createdAt;
    private String createdBy;

    private Folder() {
    }

    public static Folder create(FolderId id, CompanyId companyId, FolderId parentId, String name,
                                int sequence, String linkedModel, String systemKey,
                                String createdBy, Instant now) {
        if (companyId == null) {
            throw new DocumentsDomainException("error.documents.companyRequired", null, "companyId required");
        }
        FolderTreeRules.validateName(name);
        Folder f = new Folder();
        f.setId(id);
        f.companyId = companyId;
        f.parentId = parentId;
        f.name = name.trim();
        f.nameNormalized = NameNormalizer.normalize(name);
        f.sequence = sequence;
        f.inheritAccess = true;
        f.linkedModel = blankToNull(linkedModel);
        f.systemKey = systemKey;
        f.createdAt = now;
        f.createdBy = createdBy;
        return f;
    }

    public static Folder restore(FolderId id, CompanyId companyId, FolderId parentId, String name,
                                 String nameNormalized, int sequence, boolean archived, boolean inheritAccess,
                                 String linkedModel, String systemKey, Instant createdAt, String createdBy) {
        Folder f = new Folder();
        f.setId(id);
        f.companyId = companyId;
        f.parentId = parentId;
        f.name = name;
        f.nameNormalized = nameNormalized;
        f.sequence = sequence;
        f.archived = archived;
        f.inheritAccess = inheritAccess;
        f.linkedModel = linkedModel;
        f.systemKey = systemKey;
        f.createdAt = createdAt;
        f.createdBy = createdBy;
        return f;
    }

    public void rename(String newName) {
        FolderTreeRules.validateName(newName);
        this.name = newName.trim();
        this.nameNormalized = NameNormalizer.normalize(newName);
    }

    public void moveTo(FolderId newParentId) {
        this.parentId = newParentId;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }

    public void changeSequence(int sequence) {
        this.sequence = sequence;
    }

    public void changeInheritAccess(boolean inherit) {
        this.inheritAccess = inherit;
    }

    public void changeLinkedModel(String model) {
        this.linkedModel = blankToNull(model);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public CompanyId getCompanyId() { return companyId; }
    public FolderId getParentId() { return parentId; }
    public String getName() { return name; }
    public String getNameNormalized() { return nameNormalized; }
    public int getSequence() { return sequence; }
    public boolean isArchived() { return archived; }
    public boolean isInheritAccess() { return inheritAccess; }
    public String getLinkedModel() { return linkedModel; }
    public String getSystemKey() { return systemKey; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
