package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductPackagingId extends BaseId<UUID> {
    public ProductPackagingId(UUID value) {
        super(value);
    }
}
