package com.bradox.delin.inventory.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductCategoryId extends BaseId<UUID> {
    public ProductCategoryId(UUID value) {
        super(value);
    }
}
