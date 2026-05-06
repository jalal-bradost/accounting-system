package com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerId extends BaseId<UUID> {
    public PartnerId(UUID value) {
        super(value);
    }
}
