package com.bradox.delin.expense.domain.core.valueobject;

import com.bradox.delin.domain.valueobject.BaseId;

import java.util.UUID;

public class ExpenseId extends BaseId<UUID> {
    public ExpenseId(UUID value) {
        super(value);
    }
}
