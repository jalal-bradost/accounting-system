package com.bradox.delin.domain.core.ValueObject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class AccountId extends BaseId<UUID> {
    public AccountId(UUID value){
        super(value);
    }
}
