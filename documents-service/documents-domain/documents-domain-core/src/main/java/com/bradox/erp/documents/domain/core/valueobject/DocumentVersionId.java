package com.bradox.erp.documents.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class DocumentVersionId extends BaseId<UUID> {
    public DocumentVersionId(UUID value) {
        super(value);
    }
}
