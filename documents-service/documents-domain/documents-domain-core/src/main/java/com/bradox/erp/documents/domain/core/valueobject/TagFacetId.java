package com.bradox.erp.documents.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class TagFacetId extends BaseId<UUID> {
    public TagFacetId(UUID value) {
        super(value);
    }
}
