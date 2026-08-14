package com.jalaldeveloper.accountingsystem.expense.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.Expense;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ExpenseState;
import com.jalaldeveloper.accountingsystem.expense.domain.core.valueobject.ExpenseId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(ExpenseId id);

    List<Expense> search(CompanyId companyId, UUID employeeId, ExpenseState state);

    BigDecimal sumTotalByStates(CompanyId companyId, UUID employeeId, Collection<ExpenseState> states);
}
