package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class UomCategoryId extends BaseId<UUID> {
    public UomCategoryId(UUID value) {
        super(value);
    }
}
