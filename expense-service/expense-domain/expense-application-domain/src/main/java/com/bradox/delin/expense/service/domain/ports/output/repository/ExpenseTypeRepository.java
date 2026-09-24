package com.bradox.delin.expense.service.domain.ports.output.repository;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.entity.ExpenseType;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseTypeId;

import java.util.List;
import java.util.Optional;

public interface ExpenseTypeRepository {

    List<ExpenseType> findActiveByCompany(CompanyId companyId);

    Optional<ExpenseType> findById(ExpenseTypeId id);

    Optional<ExpenseType> findByCompanyAndNameIgnoreCase(CompanyId companyId, String name);

    ExpenseType save(ExpenseType type);
}
