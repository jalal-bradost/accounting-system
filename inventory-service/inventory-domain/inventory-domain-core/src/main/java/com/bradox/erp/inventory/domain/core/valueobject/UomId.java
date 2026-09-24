package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class UomId extends BaseId<UUID> {
    public UomId(UUID value) {
        super(value);
    }
}
