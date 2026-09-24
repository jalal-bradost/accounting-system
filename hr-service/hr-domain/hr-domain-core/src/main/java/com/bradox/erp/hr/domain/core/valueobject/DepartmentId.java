package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class DepartmentId extends BaseId<UUID> {
    public DepartmentId(UUID value) {
        super(value);
    }
}
