package com.bradox.erp.documents.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class TagId extends BaseId<UUID> {
    public TagId(UUID value) {
        super(value);
    }
}
