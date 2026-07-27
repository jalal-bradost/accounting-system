package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveRequestId extends BaseId<UUID> {
    public LeaveRequestId(UUID value) {
        super(value);
    }
}
