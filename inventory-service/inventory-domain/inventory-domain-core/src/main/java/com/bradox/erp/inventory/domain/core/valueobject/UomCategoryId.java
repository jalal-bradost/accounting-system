package com.bradox.erp.inventory.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class UomCategoryId extends BaseId<UUID> {
    public UomCategoryId(UUID value) {
        super(value);
    }
}
