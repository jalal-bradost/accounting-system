package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class WeekId extends BaseId<UUID> {
    public WeekId(UUID value) {
        super(value);
    }
}
