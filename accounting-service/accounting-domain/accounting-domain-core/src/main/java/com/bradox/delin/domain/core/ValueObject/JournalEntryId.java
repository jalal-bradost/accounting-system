package com.bradox.delin.domain.core.ValueObject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalEntryId extends BaseId<UUID> {
    public JournalEntryId(final UUID id){
        super(id);
    }
}
