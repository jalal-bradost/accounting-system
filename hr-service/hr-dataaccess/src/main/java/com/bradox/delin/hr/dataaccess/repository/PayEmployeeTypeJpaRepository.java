package com.bradox.delin.hr.dataaccess.repository;

import com.bradox.delin.hr.dataaccess.entity.PayEmployeeTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayEmployeeTypeJpaRepository extends JpaRepository<PayEmployeeTypeEntity, UUID> {
    List<PayEmployeeTypeEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
