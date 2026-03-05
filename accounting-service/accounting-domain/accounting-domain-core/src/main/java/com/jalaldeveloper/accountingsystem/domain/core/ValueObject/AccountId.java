package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class AccountId extends BaseId<UUID> {
    public AccountId(UUID value){
        super(value);
    }
}
