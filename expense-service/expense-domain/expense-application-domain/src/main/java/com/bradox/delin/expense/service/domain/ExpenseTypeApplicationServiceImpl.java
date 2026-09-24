package com.bradox.delin.expense.service.domain;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.domain.core.entity.ExpenseType;
import com.bradox.delin.expense.domain.core.exception.ExpenseDomainException;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseTypeId;
import com.bradox.delin.expense.service.domain.dto.ExpenseTypeResponse;
import com.bradox.delin.expense.service.domain.dto.SaveExpenseTypeCommand;
import com.bradox.delin.expense.service.domain.ports.input.ExpenseTypeApplicationService;
import com.bradox.delin.expense.service.domain.ports.output.repository.ExpenseTypeRepository;
import com.bradox.delin.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
class ExpenseTypeApplicationServiceImpl implements ExpenseTypeApplicationService {

    private final ExpenseTypeRepository expenseTypeRepository;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    ExpenseTypeApplicationServiceImpl(ExpenseTypeRepository expenseTypeRepository,
                                      ObjectProvider<CompanyContext> companyContextProvider) {
        this.expenseTypeRepository = expenseTypeRepository;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseTypeResponse> list(CompanyId companyId) {
        return expenseTypeRepository.findActiveByCompany(requireCompany(companyId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseTypeResponse create(SaveExpenseTypeCommand command) {
        CompanyId companyId = requireCompany(command.getCompanyId() != null
                ? new CompanyId(command.getCompanyId())
                : null);
        String name = command.getName().trim();
        return expenseTypeRepository.findByCompanyAndNameIgnoreCase(companyId, name)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    ExpenseType type = new ExpenseType(
                            new ExpenseTypeId(UUID.randomUUID()),
                            companyId,
                            name,
                            true,
                            now,
                            now);
                    type.validate();
                    return toResponse(expenseTypeRepository.save(type));
                });
    }

    @Override
    @Transactional
    public ExpenseTypeResponse update(UUID id, SaveExpenseTypeCommand command) {
        ExpenseType type = expenseTypeRepository.findById(new ExpenseTypeId(id))
                .orElseThrow(() -> new ExpenseDomainException("Expense type not found: " + id));
        type.rename(command.getName());
        type.validate();
        return toResponse(expenseTypeRepository.save(type));
    }

    private CompanyId requireCompany(CompanyId companyId) {
        if (companyId != null && companyId.getId() != null) {
            return companyId;
        }
        CompanyContext ctx = companyContextProvider.getIfAvailable();
        if (ctx != null) {
            return ctx.currentCompany()
                    .orElseThrow(() -> new ExpenseDomainException("companyId required"));
        }
        throw new ExpenseDomainException("error.expense.companyIdRequired", null, "companyId required");
    }

    private ExpenseTypeResponse toResponse(ExpenseType type) {
        ExpenseTypeResponse r = new ExpenseTypeResponse();
        r.setId(type.getId().getId());
        r.setCompanyId(type.getCompanyId().getId());
        r.setName(type.getName());
        r.setActive(type.isActive());
        r.setCreatedAt(type.getCreatedAt());
        r.setUpdatedAt(type.getUpdatedAt());
        return r;
    }
}
