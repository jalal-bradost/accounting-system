package com.bradox.erp.expense.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.expense.dataaccess.entity.ExpExpenseTypeEntity;
import com.bradox.erp.expense.dataaccess.repository.ExpenseTypeJpaRepository;
import com.bradox.erp.expense.domain.core.entity.ExpenseType;
import com.bradox.erp.expense.domain.core.valueobject.ExpenseTypeId;
import com.bradox.erp.expense.service.domain.ports.output.repository.ExpenseTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExpenseTypeRepositoryImpl implements ExpenseTypeRepository {

    private final ExpenseTypeJpaRepository jpa;

    public ExpenseTypeRepositoryImpl(ExpenseTypeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ExpenseType> findActiveByCompany(CompanyId companyId) {
        return jpa.findByCompanyIdAndActiveTrueOrderByNameAsc(companyId.getId()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ExpenseType> findById(ExpenseTypeId id) {
        return jpa.findById(id.getId()).map(this::toDomain);
    }

    @Override
    public Optional<ExpenseType> findByCompanyAndNameIgnoreCase(CompanyId companyId, String name) {
        return jpa.findByCompanyIdAndNameIgnoreCase(companyId.getId(), name).map(this::toDomain);
    }

    @Override
    public ExpenseType save(ExpenseType type) {
        ExpExpenseTypeEntity entity = jpa.findById(type.getId().getId()).orElseGet(ExpExpenseTypeEntity::new);
        entity.setId(type.getId().getId());
        entity.setCompanyId(type.getCompanyId().getId());
        entity.setName(type.getName());
        entity.setActive(type.isActive());
        entity.setCreatedAt(type.getCreatedAt());
        entity.setUpdatedAt(type.getUpdatedAt());
        return toDomain(jpa.save(entity));
    }

    private ExpenseType toDomain(ExpExpenseTypeEntity e) {
        return new ExpenseType(
                new ExpenseTypeId(e.getId()),
                new CompanyId(e.getCompanyId()),
                e.getName(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
