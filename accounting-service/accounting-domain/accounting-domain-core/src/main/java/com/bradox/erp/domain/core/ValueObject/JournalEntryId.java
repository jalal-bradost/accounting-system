package com.bradox.erp.domain.core.ValueObject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalEntryId extends BaseId<UUID> {
    public JournalEntryId(final UUID id){
        super(id);
    }
}
