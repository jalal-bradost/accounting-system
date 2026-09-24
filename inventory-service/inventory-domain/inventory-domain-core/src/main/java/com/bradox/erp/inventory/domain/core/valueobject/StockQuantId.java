package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class StockQuantId extends BaseId<UUID> {
    public StockQuantId(UUID value) {
        super(value);
    }
}
