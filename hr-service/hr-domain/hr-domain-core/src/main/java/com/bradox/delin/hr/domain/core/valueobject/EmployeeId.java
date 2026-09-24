package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class EmployeeId extends BaseId<UUID> {
    public EmployeeId(UUID value) {
        super(value);
    }
}
