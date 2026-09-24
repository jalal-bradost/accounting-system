package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveRequestId extends BaseId<UUID> {
    public LeaveRequestId(UUID value) {
        super(value);
    }
}
