package com.bradox.erp.contacts.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class PartnerBankAccountId extends BaseId<UUID> {
    public PartnerBankAccountId(UUID value) {
        super(value);
    }
}
