package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductCategoryId extends BaseId<UUID> {
    public ProductCategoryId(UUID value) {
        super(value);
    }
}
