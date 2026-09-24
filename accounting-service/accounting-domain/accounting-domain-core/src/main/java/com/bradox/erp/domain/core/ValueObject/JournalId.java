package com.bradox.erp.domain.core.ValueObject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalId extends BaseId<UUID> {
    public JournalId(UUID value){
        super(value);
    }
}
