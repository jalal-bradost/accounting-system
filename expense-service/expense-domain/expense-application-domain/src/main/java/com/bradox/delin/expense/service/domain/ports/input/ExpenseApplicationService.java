package com.bradox.delin.expense.service.domain.ports.input;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.entity.ExpenseState;
import com.bradox.delin.expense.service.domain.dto.ExpenseResponse;
import com.bradox.delin.expense.service.domain.dto.ExpenseSummaryResponse;
import com.bradox.delin.expense.service.domain.dto.RegisterExpensePaymentCommand;
import com.bradox.delin.expense.service.domain.dto.SaveExpenseCommand;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface ExpenseApplicationService {

    List<ExpenseResponse> list(CompanyId companyId, UUID employeeId, ExpenseState state);

    ExpenseSummaryResponse summary(CompanyId companyId, UUID employeeId);

    ExpenseResponse get(UUID id);

    ExpenseResponse create(@Valid SaveExpenseCommand command);

    ExpenseResponse update(UUID id, @Valid SaveExpenseCommand command);

    ExpenseResponse submit(UUID id);

    ExpenseResponse approve(UUID id);

    ExpenseResponse refuse(UUID id);

    ExpenseResponse post(UUID id);

    ExpenseResponse registerPayment(UUID id, @Valid RegisterExpensePaymentCommand command);

    ExpenseResponse cancel(UUID id);
}
