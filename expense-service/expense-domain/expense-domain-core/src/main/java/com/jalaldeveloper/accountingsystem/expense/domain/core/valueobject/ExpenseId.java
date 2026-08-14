package com.jalaldeveloper.accountingsystem.expense.domain.core.valueobject;

import com.jalaldeveloper.accountingsystem.domain.valueobject.BaseId;

import java.util.UUID;

public class ExpenseId extends BaseId<UUID> {
    public ExpenseId(UUID value) {
        super(value);
    }
}
