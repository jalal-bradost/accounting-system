package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class StockLocationId extends BaseId<UUID> {
    public StockLocationId(UUID value) {
        super(value);
    }
}
