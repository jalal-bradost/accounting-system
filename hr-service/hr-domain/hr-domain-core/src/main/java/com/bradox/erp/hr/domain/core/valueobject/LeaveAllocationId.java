package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveAllocationId extends BaseId<UUID> {
    public LeaveAllocationId(UUID value) {
        super(value);
    }
}
