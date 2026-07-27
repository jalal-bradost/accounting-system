package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveAllocationId extends BaseId<UUID> {
    public LeaveAllocationId(UUID value) {
        super(value);
    }
}
