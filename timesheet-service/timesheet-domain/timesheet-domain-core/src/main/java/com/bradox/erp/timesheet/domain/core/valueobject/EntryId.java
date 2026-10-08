package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class EntryId extends BaseId<UUID> {
    public EntryId(UUID value) {
        super(value);
    }
}
