package com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerAddressId extends BaseId<UUID> {
    public PartnerAddressId(UUID value) {
        super(value);
    }
}
