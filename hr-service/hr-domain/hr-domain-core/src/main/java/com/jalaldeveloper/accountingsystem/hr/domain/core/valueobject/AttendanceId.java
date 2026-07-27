package com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class AttendanceId extends BaseId<UUID> {
    public AttendanceId(UUID value) {
        super(value);
    }
}
