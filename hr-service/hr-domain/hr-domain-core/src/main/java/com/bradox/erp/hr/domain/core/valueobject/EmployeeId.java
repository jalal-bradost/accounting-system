package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class EmployeeId extends BaseId<UUID> {
    public EmployeeId(UUID value) {
        super(value);
    }
}
