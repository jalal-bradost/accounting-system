package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class EmployeeId extends BaseId<UUID> {
    public EmployeeId(UUID value) {
        super(value);
    }
}
