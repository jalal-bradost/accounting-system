package com.bradox.erp.documents.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class FolderId extends BaseId<UUID> {
    public FolderId(UUID value) {
        super(value);
    }
}
