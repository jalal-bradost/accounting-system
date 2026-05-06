package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class WarehouseId extends BaseId<UUID> {
    public WarehouseId(UUID value) {
        super(value);
    }
}
