package com.bradox.delin.contacts.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class PaymentTermsId extends BaseId<UUID> {
    public PaymentTermsId(UUID value) {
        super(value);
    }
}
