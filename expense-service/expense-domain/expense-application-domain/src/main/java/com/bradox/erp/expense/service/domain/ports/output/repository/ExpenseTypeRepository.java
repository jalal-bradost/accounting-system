package com.bradox.erp.expense.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.expense.domain.core.entity.ExpenseType;
import com.bradox.erp.expense.domain.core.valueobject.ExpenseTypeId;

import java.util.List;
import java.util.Optional;

public interface ExpenseTypeRepository {

    List<ExpenseType> findActiveByCompany(CompanyId companyId);

    Optional<ExpenseType> findById(ExpenseTypeId id);

    Optional<ExpenseType> findByCompanyAndNameIgnoreCase(CompanyId companyId, String name);

    ExpenseType save(ExpenseType type);
}
