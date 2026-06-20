package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class DepartmentId extends BaseId<UUID> {
    public DepartmentId(UUID value) {
        super(value);
    }
}
