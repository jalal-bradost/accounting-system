package com.bradox.erp.contacts.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerAddressId extends BaseId<UUID> {
    public PartnerAddressId(UUID value) {
        super(value);
    }
}
