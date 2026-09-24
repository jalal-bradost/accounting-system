package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class UomId extends BaseId<UUID> {
    public UomId(UUID value) {
        super(value);
    }
}
