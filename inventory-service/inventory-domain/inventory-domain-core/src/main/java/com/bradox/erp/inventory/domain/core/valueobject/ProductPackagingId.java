package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class ProductPackagingId extends BaseId<UUID> {
    public ProductPackagingId(UUID value) {
        super(value);
    }
}
