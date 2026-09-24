package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveAllocationId extends BaseId<UUID> {
    public LeaveAllocationId(UUID value) {
        super(value);
    }
}
