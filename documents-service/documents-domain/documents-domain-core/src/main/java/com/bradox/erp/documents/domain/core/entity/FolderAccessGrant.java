package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;

import java.util.UUID;

/** One grant of a level on a folder to a user or a role. */
public final class FolderAccessGrant {

    private final SubjectType subjectType;
    private final UUID subjectId;
    private final AccessLevel level;

    public FolderAccessGrant(SubjectType subjectType, UUID subjectId, AccessLevel level) {
        if (subjectType == null || subjectId == null) {
            throw new DocumentsDomainException("error.documents.grantSubjectRequired", null, "Grant subject is required");
        }
        if (level == null || level == AccessLevel.NONE) {
            throw new DocumentsDomainException("error.documents.grantLevelRequired", null,
                    "Grant level must be VIEW, EDIT or MANAGE");
        }
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.level = level;
    }

    public SubjectType getSubjectType() { return subjectType; }
    public UUID getSubjectId() { return subjectId; }
    public AccessLevel getLevel() { return level; }
}
