package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class StockLocationId extends BaseId<UUID> {
    public StockLocationId(UUID value) {
        super(value);
    }
}
