package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class StockMoveId extends BaseId<UUID> {
    public StockMoveId(UUID value) {
        super(value);
    }
}
