package com.bradox.delin.domain.core.ValueObject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalItemId extends BaseId<UUID> {
    public JournalItemId(UUID id) {
        super(id);
    }
}
