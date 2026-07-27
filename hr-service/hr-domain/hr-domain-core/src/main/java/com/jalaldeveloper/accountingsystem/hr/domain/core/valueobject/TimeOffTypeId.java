package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class TimeOffTypeId extends BaseId<UUID> {
    public TimeOffTypeId(UUID value) {
        super(value);
    }
}
