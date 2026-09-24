package com.bradox.delin.expense.domain.core.entity;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.exception.ExpenseDomainException;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseTypeId;

import java.time.Instant;

public class ExpenseType {

    private final ExpenseTypeId id;
    private final CompanyId companyId;
    private String name;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public ExpenseType(ExpenseTypeId id, CompanyId companyId, String name, boolean active,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void validate() {
        if (companyId == null) {
            throw new ExpenseDomainException("error.expenseType.companyIdRequired", null, "companyId required");
        }
        if (name == null || name.isBlank()) {
            throw new ExpenseDomainException("error.expenseType.nameRequired", null, "name required");
        }
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new ExpenseDomainException("error.expenseType.nameRequired", null, "name required");
        }
        this.name = name.trim();
        this.updatedAt = Instant.now();
    }

    public ExpenseTypeId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
