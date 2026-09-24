package com.bradox.delin.expense.dataaccess.adapter;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.expense.dataaccess.mapper.ExpenseDataAccessMapper;
import com.bradox.delin.expense.dataaccess.repository.ExpenseJpaRepository;
import com.bradox.delin.expense.domain.core.entity.Expense;
import com.bradox.delin.expense.domain.core.entity.ExpenseState;
import com.bradox.delin.expense.domain.core.valueobject.ExpenseId;
import com.bradox.delin.expense.service.domain.ports.output.repository.ExpenseRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final ExpenseJpaRepository jpaRepository;

    public ExpenseRepositoryImpl(ExpenseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Expense save(Expense expense) {
        var existing = jpaRepository.findById(expense.getId().getId()).orElse(null);
        return ExpenseDataAccessMapper.entityToDomain(
                jpaRepository.save(ExpenseDataAccessMapper.domainToEntity(expense, existing)));
    }

    @Override
    public Optional<Expense> findById(ExpenseId id) {
        return jpaRepository.findById(id.getId()).map(ExpenseDataAccessMapper::entityToDomain);
    }

    @Override
    public List<Expense> search(CompanyId companyId, UUID employeeId, ExpenseState state) {
        return jpaRepository.search(companyId.getId(), employeeId, state).stream()
                .map(ExpenseDataAccessMapper::entityToDomain)
                .toList();
    }

    @Override
    public BigDecimal sumTotalByStates(CompanyId companyId, UUID employeeId, Collection<ExpenseState> states) {
        BigDecimal sum = jpaRepository.sumTotalByStates(companyId.getId(), employeeId, states);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
