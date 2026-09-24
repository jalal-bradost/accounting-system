package com.bradox.erp.hr.dataaccess.repository;

import com.bradox.erp.hr.dataaccess.entity.PayStructureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayStructureJpaRepository extends JpaRepository<PayStructureEntity, UUID> {
    List<PayStructureEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);
    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
