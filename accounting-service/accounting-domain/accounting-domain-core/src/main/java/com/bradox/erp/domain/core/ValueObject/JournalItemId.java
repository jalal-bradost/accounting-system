package com.bradox.erp.domain.core.ValueObject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalItemId extends BaseId<UUID> {
    public JournalItemId(UUID id) {
        super(id);
    }
}
