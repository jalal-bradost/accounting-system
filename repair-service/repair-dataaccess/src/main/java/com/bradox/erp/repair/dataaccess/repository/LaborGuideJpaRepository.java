package com.bradox.erp.repair.dataaccess.repository;

import com.bradox.erp.repair.dataaccess.entity.LaborGuideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LaborGuideJpaRepository extends JpaRepository<LaborGuideEntity, UUID> {

    List<LaborGuideEntity> findByCompanyId(UUID companyId);

    List<LaborGuideEntity> findByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    List<LaborGuideEntity> findByCompanyIdAndIdIn(UUID companyId, Collection<UUID> ids);
}
