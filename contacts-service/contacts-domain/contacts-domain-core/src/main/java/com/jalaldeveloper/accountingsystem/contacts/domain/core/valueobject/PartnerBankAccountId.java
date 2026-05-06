package com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerBankAccountId extends BaseId<UUID> {
    public PartnerBankAccountId(UUID value) {
        super(value);
    }
}
