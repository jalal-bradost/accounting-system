package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class TaskId extends BaseId<UUID> {
    public TaskId(UUID value) {
        super(value);
    }
}
