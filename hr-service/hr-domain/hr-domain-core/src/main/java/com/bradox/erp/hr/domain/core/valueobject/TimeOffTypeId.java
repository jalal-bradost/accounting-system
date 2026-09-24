package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class TimeOffTypeId extends BaseId<UUID> {
    public TimeOffTypeId(UUID value) {
        super(value);
    }
}
