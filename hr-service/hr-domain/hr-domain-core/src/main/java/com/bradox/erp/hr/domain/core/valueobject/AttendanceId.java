package com.bradox.erp.hr.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class AttendanceId extends BaseId<UUID> {
    public AttendanceId(UUID value) {
        super(value);
    }
}
