package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class TimerId extends BaseId<UUID> {
    public TimerId(UUID value) {
        super(value);
    }
}
