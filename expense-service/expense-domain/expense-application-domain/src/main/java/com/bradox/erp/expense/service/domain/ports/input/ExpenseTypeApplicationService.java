package com.bradox.erp.expense.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.expense.service.domain.dto.ExpenseTypeResponse;
import com.bradox.erp.expense.service.domain.dto.SaveExpenseTypeCommand;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface ExpenseTypeApplicationService {

    List<ExpenseTypeResponse> list(CompanyId companyId);

    ExpenseTypeResponse create(@Valid SaveExpenseTypeCommand command);

    ExpenseTypeResponse update(UUID id, @Valid SaveExpenseTypeCommand command);
}
