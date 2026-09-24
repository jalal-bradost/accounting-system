package com.bradox.delin.contacts.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerAddressId extends BaseId<UUID> {
    public PartnerAddressId(UUID value) {
        super(value);
    }
}
