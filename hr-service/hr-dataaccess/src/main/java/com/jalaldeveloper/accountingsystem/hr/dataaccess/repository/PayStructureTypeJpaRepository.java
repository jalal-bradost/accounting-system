package com.jalaldeveloper.accountingsystem.hr.dataaccess.repository;

import com.jalaldeveloper.accountingsystem.hr.dataaccess.entity.PayStructureTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayStructureTypeJpaRepository extends JpaRepository<PayStructureTypeEntity, UUID> {
    List<PayStructureTypeEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
