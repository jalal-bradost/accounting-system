package com.jalaldeveloper.accountingsystem.contacts.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class PaymentTermsId extends BaseId<UUID> {
    public PaymentTermsId(UUID value) {
        super(value);
    }
}
