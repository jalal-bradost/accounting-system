package com.bradox.delin.expense.dataaccess.repository;

import com.bradox.delin.expense.dataaccess.entity.ExpExpenseTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseTypeJpaRepository extends JpaRepository<ExpExpenseTypeEntity, UUID> {

    List<ExpExpenseTypeEntity> findByCompanyIdAndActiveTrueOrderByNameAsc(UUID companyId);

    Optional<ExpExpenseTypeEntity> findByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
