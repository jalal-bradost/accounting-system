package com.bradox.erp.expense.domain.core.valueobject;

import com.bradox.erp.domain.valueobject.BaseId;

import java.util.UUID;

public class ExpenseId extends BaseId<UUID> {
    public ExpenseId(UUID value) {
        super(value);
    }
}
