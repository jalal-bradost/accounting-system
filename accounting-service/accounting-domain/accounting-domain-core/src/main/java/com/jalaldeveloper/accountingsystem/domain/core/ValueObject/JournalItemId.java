package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalItemId extends BaseId<UUID> {
    public JournalItemId(UUID id) {
        super(id);
    }
}
