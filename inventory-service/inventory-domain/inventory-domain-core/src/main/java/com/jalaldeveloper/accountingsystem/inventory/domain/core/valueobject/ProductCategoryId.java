package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductCategoryId extends BaseId<UUID> {
    public ProductCategoryId(UUID value) {
        super(value);
    }
}
