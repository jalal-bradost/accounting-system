package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class TimeOffTypeId extends BaseId<UUID> {
    public TimeOffTypeId(UUID value) {
        super(value);
    }
}
