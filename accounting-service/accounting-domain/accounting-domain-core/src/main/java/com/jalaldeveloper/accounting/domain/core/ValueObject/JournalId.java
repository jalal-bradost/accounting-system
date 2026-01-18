package com.jalaldeveloper.accounting.domain.core.ValueObject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalId extends BaseId<UUID> {
    public JournalId(UUID value){
        super(value);
    }
}
