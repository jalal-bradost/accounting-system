package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class StockPickingId extends BaseId<UUID> {
    public StockPickingId(UUID value) {
        super(value);
    }
}
