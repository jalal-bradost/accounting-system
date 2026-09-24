package com.bradox.delin.contacts.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerId extends BaseId<UUID> {
    public PartnerId(UUID value) {
        super(value);
    }
}
