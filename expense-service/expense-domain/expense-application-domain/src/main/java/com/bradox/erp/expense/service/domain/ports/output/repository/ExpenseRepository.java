package com.bradox.erp.expense.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.expense.domain.core.entity.Expense;
import com.bradox.erp.expense.domain.core.entity.ExpenseState;
import com.bradox.erp.expense.domain.core.valueobject.ExpenseId;

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
