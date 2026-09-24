package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class LeaveRequestId extends BaseId<UUID> {
    public LeaveRequestId(UUID value) {
        super(value);
    }
}
