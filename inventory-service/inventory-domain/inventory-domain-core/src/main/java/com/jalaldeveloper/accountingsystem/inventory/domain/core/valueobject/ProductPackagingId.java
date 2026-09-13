package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductPackagingId extends BaseId<UUID> {
    public ProductPackagingId(UUID value) {
        super(value);
    }
}
