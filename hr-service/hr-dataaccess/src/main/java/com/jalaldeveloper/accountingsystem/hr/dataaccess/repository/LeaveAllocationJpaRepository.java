package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.LeaveAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface LeaveAllocationJpaRepository extends JpaRepository<LeaveAllocationEntity, UUID> {

    @Query("""
        SELECT a FROM LeaveAllocationEntity a
        WHERE a.companyId = :companyId
          AND (:employeeId IS NULL OR a.employeeId = :employeeId)
        ORDER BY a.dateFrom DESC
        """)
    List<LeaveAllocationEntity> search(@Param("companyId") UUID companyId,
                                       @Param("employeeId") UUID employeeId);

    List<LeaveAllocationEntity> findByCompanyIdAndEmployeeIdAndState(UUID companyId, UUID employeeId, String state);
}
