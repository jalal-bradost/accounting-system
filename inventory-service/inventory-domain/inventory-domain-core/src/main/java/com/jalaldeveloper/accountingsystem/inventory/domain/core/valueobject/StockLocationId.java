package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class StockLocationId extends BaseId<UUID> {
    public StockLocationId(UUID value) {
        super(value);
    }
}
