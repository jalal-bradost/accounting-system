package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class UomCategoryId extends BaseId<UUID> {
    public UomCategoryId(UUID value) {
        super(value);
    }
}
