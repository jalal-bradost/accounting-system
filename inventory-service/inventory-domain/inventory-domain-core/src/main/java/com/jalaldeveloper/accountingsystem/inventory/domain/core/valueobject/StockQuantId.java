package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class StockQuantId extends BaseId<UUID> {
    public StockQuantId(UUID value) {
        super(value);
    }
}
