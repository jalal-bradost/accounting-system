package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class ProjectId extends BaseId<UUID> {
    public ProjectId(UUID value) {
        super(value);
    }
}
