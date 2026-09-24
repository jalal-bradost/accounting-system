package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class DepartmentId extends BaseId<UUID> {
    public DepartmentId(UUID value) {
        super(value);
    }
}
