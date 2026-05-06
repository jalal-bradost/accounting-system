package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class StockPickingId extends BaseId<UUID> {
    public StockPickingId(UUID value) {
        super(value);
    }
}
