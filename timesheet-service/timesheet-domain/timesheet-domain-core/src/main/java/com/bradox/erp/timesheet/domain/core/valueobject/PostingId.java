package com.bradox.erp.timesheet.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class PostingId extends BaseId<UUID> {
    public PostingId(UUID value) {
        super(value);
    }
}
