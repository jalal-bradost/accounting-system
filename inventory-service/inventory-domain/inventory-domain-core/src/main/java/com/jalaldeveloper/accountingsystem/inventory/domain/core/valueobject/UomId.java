package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class UomId extends BaseId<UUID> {
    public UomId(UUID value) {
        super(value);
    }
}
