package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class StockQuantId extends BaseId<UUID> {
    public StockQuantId(UUID value) {
        super(value);
    }
}
