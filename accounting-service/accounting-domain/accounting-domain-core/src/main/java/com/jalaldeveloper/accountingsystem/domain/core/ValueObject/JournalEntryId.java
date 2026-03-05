package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalEntryId extends BaseId<UUID> {
    public JournalEntryId(final UUID id){
        super(id);
    }
}
