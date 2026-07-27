package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayEmployeeTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayEmployeeTypeJpaRepository extends JpaRepository<PayEmployeeTypeEntity, UUID> {
    List<PayEmployeeTypeEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
