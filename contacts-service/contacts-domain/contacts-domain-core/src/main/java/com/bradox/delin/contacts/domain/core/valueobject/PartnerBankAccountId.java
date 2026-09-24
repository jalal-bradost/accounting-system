package com.bradox.delin.contacts.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerBankAccountId extends BaseId<UUID> {
    public PartnerBankAccountId(UUID value) {
        super(value);
    }
}
