package com.bradox.delin.domain.core.ValueObject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class JournalId extends BaseId<UUID> {
    public JournalId(UUID value){
        super(value);
    }
}
