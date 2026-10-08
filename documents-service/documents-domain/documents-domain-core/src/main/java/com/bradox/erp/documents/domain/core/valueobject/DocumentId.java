package com.bradox.erp.documents.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class DocumentId extends BaseId<UUID> {
    public DocumentId(UUID value) {
        super(value);
    }
}
