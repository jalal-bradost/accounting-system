package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class ValuationLayerId extends BaseId<UUID> {
    public ValuationLayerId(UUID value) {
        super(value);
    }
}
