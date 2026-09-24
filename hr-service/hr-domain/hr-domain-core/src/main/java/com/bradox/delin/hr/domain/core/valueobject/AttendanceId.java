package com.bradox.delin.hr.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class AttendanceId extends BaseId<UUID> {
    public AttendanceId(UUID value) {
        super(value);
    }
}
