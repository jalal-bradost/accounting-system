package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class StockMoveId extends BaseId<UUID> {
    public StockMoveId(UUID value) {
        super(value);
    }
}
