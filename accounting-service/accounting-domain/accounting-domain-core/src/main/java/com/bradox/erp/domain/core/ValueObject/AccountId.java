package com.bradox.erp.domain.core.ValueObject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class AccountId extends BaseId<UUID> {
    public AccountId(UUID value){
        super(value);
    }
}
