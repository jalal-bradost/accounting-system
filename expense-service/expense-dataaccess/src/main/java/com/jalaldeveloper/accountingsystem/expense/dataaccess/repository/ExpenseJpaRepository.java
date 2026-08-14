package com.jalaldeveloper.accountingsystem.expense.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.expense.dataaccess.entity.ExpExpenseEntity;
import com.jalaldeveloper.accountingsystem.expense.domain.core.entity.ExpenseState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExpenseJpaRepository extends JpaRepository<ExpExpenseEntity, UUID> {

    @Query("""
            SELECT e FROM ExpExpenseEntity e
            WHERE e.companyId = :companyId
              AND (:employeeId IS NULL OR e.employeeId = :employeeId)
              AND (:state IS NULL OR e.state = :state)
            ORDER BY e.expenseDate DESC, e.createdAt DESC
            """)
    List<ExpExpenseEntity> search(@Param("companyId") UUID companyId,
                                  @Param("employeeId") UUID employeeId,
                                  @Param("state") ExpenseState state);

    @Query("""
            SELECT COALESCE(SUM(e.total), 0) FROM ExpExpenseEntity e
            WHERE e.companyId = :companyId
              AND (:employeeId IS NULL OR e.employeeId = :employeeId)
              AND e.state IN :states
            """)
    BigDecimal sumTotalByStates(@Param("companyId") UUID companyId,
                                @Param("employeeId") UUID employeeId,
                                @Param("states") Collection<ExpenseState> states);
}
