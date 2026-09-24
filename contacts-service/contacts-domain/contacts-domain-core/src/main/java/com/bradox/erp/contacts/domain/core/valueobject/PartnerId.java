package com.bradox.erp.contacts.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerId extends BaseId<UUID> {
    public PartnerId(UUID value) {
        super(value);
    }
}
