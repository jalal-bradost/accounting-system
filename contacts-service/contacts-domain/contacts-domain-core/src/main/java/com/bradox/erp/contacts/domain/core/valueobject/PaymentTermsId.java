package com.bradox.erp.contacts.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class PaymentTermsId extends BaseId<UUID> {
    public PaymentTermsId(UUID value) {
        super(value);
    }
}
