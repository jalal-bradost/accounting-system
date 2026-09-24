package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class ValuationLayerId extends BaseId<UUID> {
    public ValuationLayerId(UUID value) {
        super(value);
    }
}
