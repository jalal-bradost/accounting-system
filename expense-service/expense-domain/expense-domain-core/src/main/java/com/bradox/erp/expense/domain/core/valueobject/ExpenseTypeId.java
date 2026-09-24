package com.bradox.erp.expense.domain.core.valueobject;

import java.util.UUID;

public class ExpenseTypeId {

    private final UUID id;

    public ExpenseTypeId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
