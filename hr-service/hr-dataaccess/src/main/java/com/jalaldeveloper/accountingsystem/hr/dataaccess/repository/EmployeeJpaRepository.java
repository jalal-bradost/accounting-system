package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.EmployeeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {

    @Query("""
        SELECT e FROM EmployeeEntity e
        WHERE e.companyId = :companyId
          AND (:departmentId IS NULL OR e.departmentId = :departmentId)
          AND (:includeArchived = TRUE OR e.active = TRUE)
          AND (
                :query IS NULL OR :query = ''
                OR LOWER(e.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.workEmail, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.jobTitle, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.workPhone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(e.mobilePhone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY e.displayName ASC
        """)
    Page<EmployeeEntity> search(@Param("companyId") UUID companyId,
                                @Param("query") String query,
                                @Param("departmentId") UUID departmentId,
                                @Param("includeArchived") boolean includeArchived,
                                Pageable pageable);

    long countByDepartmentIdAndActiveTrue(UUID departmentId);
}
